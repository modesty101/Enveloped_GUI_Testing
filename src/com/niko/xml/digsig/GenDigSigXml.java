package com.niko.xml.digsig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GenDigSigXml {
	/**
	 * Method used to get the XML document by parsing
	 *
	 * @param xmlFilePath
	 *            , file path of the XML document
	 * @return Document
	 */
	public static Document getXmlDocument(String xmlFilePath) {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		try {
			doc = dbf.newDocumentBuilder().parse(new FileInputStream(xmlFilePath));
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (SAXException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return doc;
	}

	/**
	 * Method used to get the KeyInfo
	 *
	 * @param xmlSigFactory
	 * @param publicKeyPath
	 * @return KeyInfo
	 */
	private static KeyInfo getKeyInfo(XMLSignatureFactory xmlSigFactory, String publicKeyPath) {
		KeyInfo keyInfo = null;
		KeyValue keyValue = null;
		PublicKey publicKey = new Nrypto().storedPublicKey(publicKeyPath);
		KeyInfoFactory keyInfoFact = xmlSigFactory.getKeyInfoFactory();

		try {
			keyValue = keyInfoFact.newKeyValue(publicKey);
		} catch (KeyException ex) {
			ex.printStackTrace();
		}
		keyInfo = keyInfoFact.newKeyInfo(Collections.singletonList(keyValue));
		return keyInfo;
	}

	/*
	 * Method used to store the signed XMl document
	 */
	private static void storeSignedDoc(Document doc, String destnSignedXmlFilePath) {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer trans = null;
		try {
			trans = transFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			ex.printStackTrace();
		}
		try {
			StreamResult streamRes = new StreamResult(new File(destnSignedXmlFilePath));
			trans.transform(new DOMSource(doc), streamRes);
		} catch (TransformerException ex) {
			ex.printStackTrace();
		}
		System.out.println("XML file with attached digital signature generated successfully ...");
	}

	/**
	 * Method used to attach a generated digital signature to the existing
	 * document
	 *
	 * @param originalXmlFilePath
	 * @param destnSignedXmlFilePath
	 * @param privateKeyFilePath
	 * @param publicKeyFilePath
	 */
	public static void generateXMLDigitalSignature(String originalXmlFilePath, String destnSignedXmlFilePath,
			String privateKeyFilePath, String publicKeyFilePath) {
		// Get the XML Document object
		Document doc = getXmlDocument(originalXmlFilePath);
		// Create XML Signature Factory
		XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance("DOM");
		PrivateKey privateKey = new Nrypto().storedPrivateKey(privateKeyFilePath);
		DOMSignContext domSignCtx = new DOMSignContext(privateKey, doc.getDocumentElement());
		Reference ref = null;
		SignedInfo signedInfo = null;
		try {
			ref = xmlSigFactory.newReference("", xmlSigFactory.newDigestMethod(DigestMethod.SHA1, null),
					Collections.singletonList(
							xmlSigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
					null, null);
			signedInfo = xmlSigFactory.newSignedInfo(
					xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
							(C14NMethodParameterSpec) null),
					xmlSigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (InvalidAlgorithmParameterException ex) {
			ex.printStackTrace();
		}
		// Pass the Public Key File Path
		KeyInfo keyInfo = getKeyInfo(xmlSigFactory, publicKeyFilePath);
		// Create a new XML Signature
		XMLSignature xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo);
		try {
			// Sign the document
			xmlSignature.sign(domSignCtx);
		} catch (MarshalException ex) {
			ex.printStackTrace();
		} catch (XMLSignatureException ex) {
			ex.printStackTrace();
		}
		// Store the digitally signed document inta a location
		storeSignedDoc(doc, destnSignedXmlFilePath);
	}
	
	/**
	 * 전자서명(XML)을 검증합니다.
	 * 
	 * @param
	 * @param
	 * @return true or false (boolean)
	 * @throws Exception
	 */
	public static boolean isDigitalSignatureValid(String signedXmlPath, String publicKeyPath) throws Exception {
		boolean validFlag = false;
		Document doc = getXmlDocument(signedXmlPath);
		NodeList nL = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nL.getLength() == 0) {
			throw new Exception("No XML Digital Signature Found ! , Document is defaced.");
		}
		PublicKey publicKey = new Nrypto().storedPublicKey(publicKeyPath);
		DOMValidateContext validContext = new DOMValidateContext(publicKey, nL.item(0));
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
		XMLSignature signature = fac.unmarshalXMLSignature(validContext);
		validFlag = signature.validate(validContext);
		return validFlag;
	}
}
