# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Firma digital opcional (XMLDSig enveloped, RSA-SHA256) del XML generado.

Se crea un certificado autofirmado de prueba en memoria (nunca se guarda ni se versiona un .pfx real).
"""
import base64
import datetime as dt
import json
from pathlib import Path

import pytest
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID
from lxml import etree

from facturacion_xml import (FirmaDigitalError, ValidadorXsd, firmar_xml, generar_xml_factura,
                             verificar_firma)
from facturacion_xml.__main__ import main

RAIZ = Path(__file__).resolve().parents[1]
EJEMPLO = RAIZ / "docs" / "ejemplos" / "factura-abarrotes.json"
CLAVE = "clave-de-prueba"
NS = {"ds": "http://www.w3.org/2000/09/xmldsig#",
      "ext": "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
      "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
      "cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"}


def _certificado(llave):
    nombre = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "ABC SOFTWARE CORP (PRUEBA)")])
    ahora = dt.datetime.now(dt.timezone.utc)
    return (x509.CertificateBuilder().subject_name(nombre).issuer_name(nombre).public_key(llave.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(ahora - dt.timedelta(days=1))
            .not_valid_after(ahora + dt.timedelta(days=30)).sign(llave, hashes.SHA256()))


@pytest.fixture(scope="module")
def pfx_y_certificado():
    llave = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    cert = _certificado(llave)
    pfx = pkcs12.serialize_key_and_certificates(
        b"prueba", llave, cert, None, serialization.BestAvailableEncryption(CLAVE.encode()))
    return pfx, cert


@pytest.fixture(scope="module")
def xml_sin_firmar():
    return generar_xml_factura(json.loads(EJEMPLO.read_text(encoding="utf-8")), 1)


@pytest.fixture(scope="module")
def xml_firmado(xml_sin_firmar, pfx_y_certificado):
    return firmar_xml(xml_sin_firmar, pfx_y_certificado[0], CLAVE)


def test_el_xml_firmado_sigue_siendo_valido_contra_el_xsd(xml_firmado):
    ValidadorXsd().validar(xml_firmado)


def test_la_firma_es_verificable(xml_firmado):
    assert verificar_firma(xml_firmado) is True


def test_ubl_extensions_es_el_primer_hijo_de_la_raiz(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    assert raiz[0].tag == "{%s}UBLExtensions" % NS["ext"]
    assert raiz[1].tag == "{%s}UBLVersionID" % NS["cbc"]
    assert len(raiz.findall(".//ds:Signature", NS)) == 1
    assert raiz.find("ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/ds:Signature", NS) is not None


def test_el_id_de_la_firma_coincide_con_cac_signature(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    uri = raiz.findtext("cac:Signature/cac:DigitalSignatureAttachment/cac:ExternalReference/cbc:URI", namespaces=NS)
    assert raiz.find(".//ds:Signature", NS).get("Id") == uri.lstrip("#")


def test_algoritmos_de_la_firma(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    metodo = lambda ruta: raiz.find(ruta, NS).get("Algorithm")  # noqa: E731
    assert metodo(".//ds:CanonicalizationMethod") == "http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
    assert metodo(".//ds:SignatureMethod") == "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"
    assert metodo(".//ds:DigestMethod") == "http://www.w3.org/2001/04/xmlenc#sha256"
    assert metodo(".//ds:Transform") == "http://www.w3.org/2000/09/xmldsig#enveloped-signature"
    assert raiz.find(".//ds:Reference", NS).get("URI") == ""


def test_keyinfo_lleva_el_certificado_entregado(xml_firmado, pfx_y_certificado):
    raiz = etree.fromstring(xml_firmado)
    der = base64.b64decode(raiz.findtext(".//ds:X509Certificate", namespaces=NS))
    assert der == pfx_y_certificado[1].public_bytes(serialization.Encoding.DER)


def test_firmar_no_cambia_el_resto_del_documento(xml_sin_firmar, xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    extensiones = raiz.find("ext:UBLExtensions", NS)
    raiz.remove(extensiones)
    original = etree.fromstring(xml_sin_firmar)
    for marca in original.xpath("comment()"):  # la marca "FIRMAR AQUÍ" desaparece al firmar
        original.remove(marca)
    assert etree.tostring(raiz, method="c14n") == etree.tostring(original, method="c14n")


def test_la_firma_cambia_si_cambia_el_comprobante(xml_sin_firmar, pfx_y_certificado):
    otro = json.loads(EJEMPLO.read_text(encoding="utf-8"))
    otro["items"][0]["cantidad"] = "11"
    firmado = firmar_xml(generar_xml_factura(otro, 1), pfx_y_certificado[0], CLAVE)
    original = firmar_xml(xml_sin_firmar, pfx_y_certificado[0], CLAVE)
    valor = lambda x: etree.fromstring(x).findtext(".//ds:SignatureValue", namespaces=NS)  # noqa: E731
    assert valor(firmado) != valor(original)


# ------------------------------------------------------------------ detección de alteraciones
def test_un_importe_alterado_invalida_la_firma(xml_firmado):
    alterado = xml_firmado.replace(b'<cbc:PayableAmount currencyID="PEN">2342.35',
                                   b'<cbc:PayableAmount currencyID="PEN">1.00')
    assert alterado != xml_firmado
    assert verificar_firma(alterado) is False


def test_una_firma_alterada_se_detecta(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    valor = raiz.find(".//ds:SignatureValue", NS)
    cifrado = bytearray(base64.b64decode(valor.text))
    cifrado[0] ^= 0xFF
    valor.text = base64.b64encode(bytes(cifrado)).decode()
    assert verificar_firma(etree.tostring(raiz)) is False


def test_un_digest_alterado_se_detecta(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    raiz.find(".//ds:DigestValue", NS).text = base64.b64encode(b"x" * 32).decode()
    assert verificar_firma(etree.tostring(raiz)) is False


def test_un_signedinfo_alterado_se_detecta(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    raiz.find(".//ds:CanonicalizationMethod", NS).set("Algorithm", "otro")
    assert verificar_firma(etree.tostring(raiz)) is False


# ------------------------------------------------------------------ errores
def test_contrasena_incorrecta(xml_sin_firmar, pfx_y_certificado):
    with pytest.raises(FirmaDigitalError, match="contraseña"):
        firmar_xml(xml_sin_firmar, pfx_y_certificado[0], "otra-clave")


def test_pfx_invalido(xml_sin_firmar):
    with pytest.raises(FirmaDigitalError):
        firmar_xml(xml_sin_firmar, b"no es un pfx", CLAVE)


def test_no_se_firma_dos_veces(xml_firmado, pfx_y_certificado):
    with pytest.raises(FirmaDigitalError, match="ya contiene"):
        firmar_xml(xml_firmado, pfx_y_certificado[0], CLAVE)


def test_xml_mal_formado(pfx_y_certificado):
    with pytest.raises(FirmaDigitalError, match="bien formado"):
        firmar_xml(b"<Invoice>", pfx_y_certificado[0], CLAVE)


def test_solo_se_admiten_claves_rsa(xml_sin_firmar):
    llave = ec.generate_private_key(ec.SECP256R1())
    nombre = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "EC")])
    ahora = dt.datetime.now(dt.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(nombre).issuer_name(nombre).public_key(llave.public_key())
            .serial_number(1).not_valid_before(ahora).not_valid_after(ahora + dt.timedelta(days=1))
            .sign(llave, hashes.SHA256()))
    pfx = pkcs12.serialize_key_and_certificates(b"ec", llave, cert, None, serialization.NoEncryption())
    with pytest.raises(FirmaDigitalError, match="RSA"):
        firmar_xml(xml_sin_firmar, pfx, None)


def test_verificar_sin_firma(xml_sin_firmar):
    with pytest.raises(FirmaDigitalError, match="no contiene"):
        verificar_firma(xml_sin_firmar)


def test_verificar_firma_incompleta(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    raiz.find(".//ds:SignatureValue", NS).text = ""
    with pytest.raises(FirmaDigitalError, match="incompleta"):
        verificar_firma(etree.tostring(raiz))


def test_verificar_firma_con_datos_no_decodificables(xml_firmado):
    raiz = etree.fromstring(xml_firmado)
    raiz.find(".//ds:X509Certificate", NS).text = "no-es-base64!"
    with pytest.raises(FirmaDigitalError, match="decodificar"):
        verificar_firma(etree.tostring(raiz))


# ------------------------------------------------------------------ línea de comandos
def test_cli_genera_y_firma(tmp_path, monkeypatch, pfx_y_certificado):
    pfx = tmp_path / "prueba.pfx"
    pfx.write_bytes(pfx_y_certificado[0])
    salida = tmp_path / "F001-00000001.xml"
    monkeypatch.setenv("FIRMA_CLAVE", CLAVE)
    assert main([str(EJEMPLO), str(salida), "--correlativo", "1", "--firmar", str(pfx)]) == 0
    assert verificar_firma(salida.read_bytes()) is True


def test_cli_con_clave_incorrecta_no_escribe_el_archivo(tmp_path, monkeypatch, pfx_y_certificado, capsys):
    pfx = tmp_path / "prueba.pfx"
    pfx.write_bytes(pfx_y_certificado[0])
    salida = tmp_path / "salida.xml"
    monkeypatch.setenv("FIRMA_CLAVE", "mala")
    assert main([str(EJEMPLO), str(salida), "--correlativo", "1", "--firmar", str(pfx)]) == 1
    assert not salida.exists()
    assert "contraseña" in capsys.readouterr().err


def test_cli_con_pfx_inexistente(tmp_path, capsys):
    assert main([str(EJEMPLO), str(tmp_path / "x.xml"), "--correlativo", "1", "--firmar", str(tmp_path / "no.pfx")]) == 1
    assert "ERROR" in capsys.readouterr().err
