/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2servlet.sbd;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.busdox.servicemetadata.publishing._1.EndpointType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.SBDMarshaller;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.commons.GlobalDebug;
import com.helger.commons.lang.ServiceLoaderUtils;
import com.helger.peppol.identifier.doctype.IPeppolReadonlyDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolReadonlyParticipantIdentifier;
import com.helger.peppol.identifier.process.IPeppolReadonlyProcessIdentifier;
import com.helger.peppol.sbdh.DocumentData;
import com.helger.peppol.sbdh.read.DocumentDataReader;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpclient.SMPClientReadonly;
import com.helger.peppol.utils.CertificateUtils;

/**
 * This processor module triggers the processing of the incoming SBD XML
 * (Standard Business Document) document.
 *
 * @author Philip Helger
 */
public final class AS2ServletSBDModule extends AbstractProcessorModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2ServletSBDModule.class);

  private final List <IAS2IncomingSBDHandlerSPI> m_aHandlers;

  public AS2ServletSBDModule ()
  {
    m_aHandlers = ServiceLoaderUtils.getAllSPIImplementations (IAS2IncomingSBDHandlerSPI.class);
    if (m_aHandlers.isEmpty ())
    {
      s_aLogger.warn ("No SPI handler of type " +
                      IAS2IncomingSBDHandlerSPI.class.getName () +
                      " for incoming SBD documents is registered. Therefore incoming documents will NOT be handled and maybe discarded!");
    }
  }

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    // Using the store action, because this action is automatically called upon
    // receipt
    return IProcessorStorageModule.DO_STORE.equals (sAction) && aMsg instanceof AS2Message;
  }

  /**
   * @param aRecipientID
   *        PEPPOL Recipient ID
   * @param aDocTypeID
   *        PEPPOL document type ID
   * @param aProcessID
   *        PEPPOL process ID
   * @return The access point URL to be used or <code>null</code>
   * @throws OpenAS2Exception
   *         In case the endpoint address could not be resolved.
   */
  @Nullable
  private static EndpointType _getRecipientEndpoint (@Nullable final IPeppolReadonlyParticipantIdentifier aRecipientID,
                                                     @Nullable final IPeppolReadonlyDocumentTypeIdentifier aDocTypeID,
                                                     @Nullable final IPeppolReadonlyProcessIdentifier aProcessID,
                                                     @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    if (aRecipientID == null || aDocTypeID == null || aProcessID == null)
      return null;

    try
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug (sMessageID + " Looking up the endpoint of recipient " + aRecipientID.getURIEncoded ());

      // Query the SMP
      SMPClientReadonly aSMPClient;
      if (s_aDirectSMPURI != null)
        aSMPClient = new SMPClientReadonly (s_aDirectSMPURI);
      else
        aSMPClient = new SMPClientReadonly (aRecipientID, SML_INFO);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug (sMessageID + " Performing SMP lookup at " + aSMPClient.getSMPHostURI ());

      return aSMPClient.getEndpoint (aRecipientID, aDocTypeID, aProcessID, ESMPTransportProfile.TRANSPORT_PROFILE_AS2);
    }
    catch (final Throwable t)
    {
      throw new OpenAS2Exception (sMessageID +
                                  " Failed to retrieve endpoint of recipient " +
                                  aRecipientID.getURIEncoded (), t);
    }
  }

  private static void _checkIfRecipientEndpointURLMatches (@Nonnull final EndpointType aRecipientEndpoint,
                                                           @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    // Get our public endpoint address from the config file
    final String sOwnAPUrl = ServerConfigFile.getOwnAPURL ();
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Our AP URL is " + sOwnAPUrl);

    // In debug mode, use our recipient URL, so that the URL check will work
    final String sRecipientAPUrl = GlobalDebug.isDebugMode () ? sOwnAPUrl
                                                             : SMPClientReadonly.getEndpointAddress (aRecipientEndpoint);
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Recipient AP URL is " + sRecipientAPUrl);

    // Is it for us?
    if (sRecipientAPUrl == null || !sRecipientAPUrl.contains (sOwnAPUrl))
    {
      s_aLogger.error (sMessageID + " The received document is not for us!");
      s_aLogger.error (sMessageID + " Request is for: " + sRecipientAPUrl);
      s_aLogger.error (sMessageID + "    Our URL is: " + sOwnAPUrl);

      // Avoid endless loop
      throw new OpenAS2Exception (sMessageID +
                                  " Internal error: The request is targeted for '" +
                                  sRecipientAPUrl +
                                  "' and is not for us (" +
                                  sOwnAPUrl +
                                  ")");
    }
  }

  private static void _checkIfEndpointCertificateMatches (@Nonnull final EndpointType aRecipientEndpoint,
                                                          @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    X509Certificate aRecipientSMPCert = null;
    try
    {
      aRecipientSMPCert = CertificateUtils.convertStringToCertficate (sRecipientCertString);
    }
    catch (final CertificateException t)
    {
      throw new OpenAS2Exception (sMessageID +
                                  " Internal error: Failed to convert looked up endpoint certificate string '" +
                                  sRecipientCertString +
                                  "' to an X.509 certificate!", t);
    }

    if (aRecipientSMPCert == null)
    {
      // No certificate found - most likely because of invalid SMP entry
      throw new OpenAS2Exception (sMessageID +
                                  " No certificate found in looked up endpoint! Is this AP maybe NOT contained in an SMP?");
    }

    // Certificate found
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Recipient certificate present: " + aRecipientSMPCert.toString ());

    if (GlobalDebug.isDebugMode ())
    {
      s_aLogger.info (sMessageID + " In debug mode the certificate is always approved");
    }
    else
    {
      // Compare serial numbers
      final BigInteger aMySerial = s_aConfiguredCert.getSerialNumber ();
      final BigInteger aReceiverSerial = aRecipientSMPCert.getSerialNumber ();
      if (!aMySerial.equals (aReceiverSerial))
      {
        s_aLogger.error (sMessageID +
                         " Certificate retrieved from SMP lookup (" +
                         aRecipientSMPCert +
                         ") does not match this APs configured Certificate (" +
                         s_aConfiguredCert +
                         ") - different serial numbers - ignoring document");
        throw new OpenAS2Exception (sMessageID +
                                    " Internal error: Looked up certificate does not match this AP certificate");
      }

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug (sMessageID + " The certificate of the recipient matches our certificate");
    }
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    try
    {
      // Interpret content as SBD
      final StandardBusinessDocument aSBD = new SBDMarshaller ().read (aMsg.getData ().getInputStream ());
      if (aSBD == null)
        throw new IllegalArgumentException ("Failed to interpret the passed document as a Standard Business Document!");

      if (false)
      {
        final DocumentData aDD = new DocumentDataReader ().extractData (aSBD);
        final String sMessageID = aDD.getInstanceIdentifier ();

        // Get the endpoint information required from the recipient
        final EndpointType aRecipientEndpoint = _getRecipientEndpoint (aDD.getReceiverAsIdentifier (),
                                                                       aDD.getDocumentTypeAsIdentifier (),
                                                                       aDD.getProcessAsIdentifier (),
                                                                       sMessageID);

        if (aRecipientEndpoint == null)
        {
          if (GlobalDebug.isDebugMode ())
          {
            // warn but continue!
            s_aLogger.warn (sMessageID +
                            " No Metadata certificate found! Is this AP maybe NOT contained in an SMP? Accepting this only because DEBUG mode is active.");
          }
          else
          {
            throw new OpenAS2Exception (sMessageID +
                                        " Failed to resolve endpoint for provided receiver/documentType/process - not handling document");
          }
        }
        else
        {
          // Check if the message is for us
          _checkIfRecipientEndpointURLMatches (aRecipientEndpoint, sMessageID);

          // Get the recipient certificate from the SMP
          _checkIfEndpointCertificateMatches (aRecipientEndpoint, sMessageID);
        }
      }

      // Handle incoming document via SPI
      for (final IAS2IncomingSBDHandlerSPI aHandler : m_aHandlers)
        aHandler.handleIncomingSBD (aSBD);
    }
    catch (final Exception ex)
    {
      // Something went wrong
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }
}
