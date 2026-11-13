# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Firma digital opcional del XML: XMLDSig *enveloped* (RSA-SHA256) en ``ext:UBLExtensions``.

El generador entrega el XML **sin firma** (el XSD de UBL la declara opcional). Este módulo es el
adaptador de ``FirmaDigitalPort``: toma ese XML y un certificado ``.pfx``/``.p12`` (X.509) y agrega
el bloque que SUNAT espera (Guía XML, "Firma digital"):

    Invoice
    └── ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent
        └── ds:Signature  Id = fragmento de cac:Signature/.../cbc:URI

Algoritmos: canonicalización C14N 1.0 (inclusiva, sin comentarios), transformación
*enveloped-signature*, digest SHA-256 y firma ``rsa-sha256``; ``KeyInfo`` lleva el certificado.
La clave privada nunca se guarda ni se escribe: se lee del ``.pfx`` y se descarta.

⚠️ Por verificar con el OSE: el firmador de producción de SUNAT/OSE puede exigir un perfil exacto
(por ejemplo, ``Id`` fijo). Cualquier OSE (p. ej. probar-xml.nubefact.com) también puede firmar por
su cuenta; en ese caso no hace falta usar este módulo.
"""
from __future__ import annotations

import base64

from cryptography import x509
from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from lxml import etree

from .errores import FirmaDigitalError

NS_DS = "http://www.w3.org/2000/09/xmldsig#"
NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2"
NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"

ALG_C14N = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
ALG_ENVELOPED = "http://www.w3.org/2000/09/xmldsig#enveloped-signature"
ALG_RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"
ALG_SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256"

ID_FIRMA_POR_DEFECTO = "SignatureSP"


def _ds(nombre: str) -> str:
    return f"{{{NS_DS}}}{nombre}"


def _ext(nombre: str) -> str:
    return f"{{{NS_EXT}}}{nombre}"


def _c14n(nodo: etree._Element) -> bytes:
    return etree.tostring(nodo, method="c14n", exclusive=False, with_comments=False)


def _cargar_pfx(pfx: bytes, clave: str | bytes | None):
    if isinstance(clave, str):
        clave = clave.encode("utf-8")
    try:
        llave, certificado, _ = pkcs12.load_key_and_certificates(pfx, clave)
    except (ValueError, TypeError) as exc:
        raise FirmaDigitalError("No se pudo abrir el certificado: archivo .pfx/.p12 inválido o contraseña incorrecta.") from exc
    if llave is None or certificado is None:
        raise FirmaDigitalError("El archivo .pfx/.p12 no contiene la clave privada y el certificado.")
    if not isinstance(llave, rsa.RSAPrivateKey):
        raise FirmaDigitalError("La firma de comprobantes SUNAT usa RSA; el certificado entregado usa otro algoritmo.")
    return llave, certificado


def _id_de_firma(raiz: etree._Element) -> str:
    """Id de ``ds:Signature``: el fragmento que ``cac:Signature`` ya referencia (``#SignatureSP<RUC>``)."""
    uri = raiz.find(f"{{{NS_CAC}}}Signature/{{{NS_CAC}}}DigitalSignatureAttachment/"
                    f"{{{NS_CAC}}}ExternalReference/{{{NS_CBC}}}URI")
    if uri is not None and uri.text and uri.text.startswith("#") and len(uri.text) > 1:
        return uri.text[1:]
    return ID_FIRMA_POR_DEFECTO


def _extension_contenido(raiz: etree._Element) -> etree._Element:
    """Crea ``ext:UBLExtensions`` como primer hijo (orden de ``InvoiceType``) y devuelve ``ExtensionContent``."""
    for marca in raiz.xpath("comment()"):
        if "FIRMAR AQUÍ" in (marca.text or ""):
            raiz.remove(marca)  # ya no hace falta: el XML queda firmado
    extensiones = raiz.find(_ext("UBLExtensions"))
    if extensiones is None:
        extensiones = etree.Element(_ext("UBLExtensions"))
        extensiones.tail = "\n  "
        raiz.insert(0, extensiones)
    extension = etree.SubElement(extensiones, _ext("UBLExtension"))
    return etree.SubElement(extension, _ext("ExtensionContent"))


def firmar_xml(xml: bytes, pfx: bytes, clave: str | bytes | None = None) -> bytes:
    """Devuelve el XML con la firma XMLDSig agregada.

    :param xml: XML generado por :func:`facturacion_xml.generar_xml_factura` (sin firmar).
    :param pfx: contenido del archivo ``.pfx``/``.p12``.
    :param clave: contraseña del archivo.
    :raises FirmaDigitalError: certificado inválido, contraseña errónea o XML ya firmado.
    """
    llave, certificado = _cargar_pfx(pfx, clave)
    try:
        raiz = etree.fromstring(xml)
    except etree.XMLSyntaxError as exc:
        raise FirmaDigitalError(f"El XML a firmar no está bien formado: {exc}") from exc
    if raiz.find(f".//{_ds('Signature')}") is not None:
        raise FirmaDigitalError("El XML ya contiene una firma digital.")

    contenido = _extension_contenido(raiz)

    # Digest del documento sin la firma (transformación enveloped): ExtensionContent está vacío todavía.
    digest = hashes.Hash(hashes.SHA256())
    digest.update(_c14n(raiz))
    valor_digest = base64.b64encode(digest.finalize()).decode("ascii")

    id_firma = _id_de_firma(raiz)
    firma = etree.SubElement(contenido, _ds("Signature"), {"Id": id_firma}, nsmap={"ds": NS_DS})
    info = etree.SubElement(firma, _ds("SignedInfo"))
    etree.SubElement(info, _ds("CanonicalizationMethod"), {"Algorithm": ALG_C14N})
    etree.SubElement(info, _ds("SignatureMethod"), {"Algorithm": ALG_RSA_SHA256})
    referencia = etree.SubElement(info, _ds("Reference"), {"URI": ""})
    transformaciones = etree.SubElement(referencia, _ds("Transforms"))
    etree.SubElement(transformaciones, _ds("Transform"), {"Algorithm": ALG_ENVELOPED})
    etree.SubElement(referencia, _ds("DigestMethod"), {"Algorithm": ALG_SHA256})
    etree.SubElement(referencia, _ds("DigestValue")).text = valor_digest

    # La firma cubre SignedInfo canonicalizado dentro del documento (con los espacios de nombres heredados).
    valor_firma = llave.sign(_c14n(info), padding.PKCS1v15(), hashes.SHA256())
    etree.SubElement(firma, _ds("SignatureValue")).text = base64.b64encode(valor_firma).decode("ascii")
    key_info = etree.SubElement(firma, _ds("KeyInfo"))
    x509_data = etree.SubElement(key_info, _ds("X509Data"))
    der = certificado.public_bytes(serialization.Encoding.DER)
    etree.SubElement(x509_data, _ds("X509Certificate")).text = base64.b64encode(der).decode("ascii")

    return b'<?xml version="1.0" encoding="UTF-8"?>\n' + etree.tostring(raiz, encoding="UTF-8")


def verificar_firma(xml: bytes) -> bool:
    """Comprueba la firma embebida: digest del documento y firma RSA con el certificado de ``KeyInfo``.

    Devuelve ``False`` si el XML se alteró después de firmarlo o si la firma no es válida; lanza
    :class:`FirmaDigitalError` si el XML no tiene una firma que se pueda leer.
    """
    raiz = etree.fromstring(xml)
    firma = raiz.find(f".//{_ds('Signature')}")
    if firma is None:
        raise FirmaDigitalError("El XML no contiene ds:Signature.")
    info = firma.find(_ds("SignedInfo"))
    valor_firma = firma.findtext(_ds("SignatureValue"))
    valor_digest = firma.findtext(f"{_ds('SignedInfo')}/{_ds('Reference')}/{_ds('DigestValue')}")
    certificado_b64 = firma.findtext(f"{_ds('KeyInfo')}/{_ds('X509Data')}/{_ds('X509Certificate')}")
    if info is None or not valor_firma or not valor_digest or not certificado_b64:
        raise FirmaDigitalError("La firma está incompleta (SignedInfo, SignatureValue, DigestValue o certificado).")
    try:
        certificado = x509.load_der_x509_certificate(base64.b64decode(certificado_b64))
        publica = certificado.public_key()
        firma_bytes = base64.b64decode(valor_firma)
        digest_esperado = base64.b64decode(valor_digest)
    except (ValueError, TypeError) as exc:
        raise FirmaDigitalError("La firma tiene datos que no se pueden decodificar.") from exc

    # El digest se calcula sobre el documento sin la firma (enveloped).
    copia = etree.fromstring(xml)
    sobrante = copia.find(f".//{_ds('Signature')}")
    sobrante.getparent().remove(sobrante)
    digest = hashes.Hash(hashes.SHA256())
    digest.update(_c14n(copia))
    if digest.finalize() != digest_esperado:
        return False
    try:
        publica.verify(firma_bytes, _c14n(info), padding.PKCS1v15(), hashes.SHA256())
    except InvalidSignature:
        return False
    return True
