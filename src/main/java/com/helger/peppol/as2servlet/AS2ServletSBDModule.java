/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.as2servlet;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.utils.CertificateHelper;
import com.helger.sbdh.SBDMarshaller;

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
    m_aHandlers = ServiceLoaderHelper.getAllSPIImplementations (IAS2IncomingSBDHandlerSPI.class);
    if (m_aHandlers.isEmpty ())
    {
      s_aLogger.warn ("No SPI handler of type " +
                      IAS2IncomingSBDHandlerSPI.class.getName () +
                      " for incoming SBD documents is registered. Therefore incoming documents will NOT be handled and maybe discarded if no other processors are active!");
    }
    else
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Loaded " + m_aHandlers.size () + " IAS2IncomingSBDHandlerSPI implementations");
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
  private static EndpointType _getReceiverEndpoint (@Nullable final IPeppolParticipantIdentifier aRecipientID,
                                                    @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                    @Nullable final IPeppolProcessIdentifier aProcessID,
                                                    @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    // Get configured client
    final SMPClientReadOnly aSMPClient = AS2PeppolServletConfiguration.getSMPClient ();
    if (aSMPClient == null)
      throw new OpenAS2Exception (sMessageID + " No SMP client configured!");

    if (aRecipientID == null || aDocTypeID == null || aProcessID == null)
      return null;

    try
    {
      if (s_aLogger.isDebugEnabled ())
      {
        s_aLogger.debug (sMessageID +
                         " Looking up the endpoint of recipient " +
                         aRecipientID.getURIEncoded () +
                         " at SMP URL '" +
                         aSMPClient.getSMPHostURI () +
                         "' for " +
                         aRecipientID.getURIEncoded () +
                         " and " +
                         aDocTypeID.getURIEncoded () +
                         " and " +
                         aProcessID.getURIEncoded ());
      }

      // Query the SMP
      return aSMPClient.getEndpoint (aRecipientID, aDocTypeID, aProcessID, ESMPTransportProfile.TRANSPORT_PROFILE_AS2);
    }
    catch (final Throwable t)
    {
      throw new OpenAS2Exception (sMessageID +
                                  " Failed to retrieve endpoint of recipient " +
                                  aRecipientID.getURIEncoded (),
                                  t);
    }
  }

  private static void _checkIfReceiverEndpointURLMatches (@Nonnull final EndpointType aRecipientEndpoint,
                                                          @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    // Get our public endpoint address from the configuration
    final String sOwnAPUrl = AS2PeppolServletConfiguration.getAS2EndpointURL ();
    if (StringHelper.hasNoText (sOwnAPUrl))
      throw new OpenAS2Exception ("The endpoint URL of this AP is not configured!");

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Our AP URL is " + sOwnAPUrl);

    final String sRecipientAPUrl = SMPClientReadOnly.getEndpointAddress (aRecipientEndpoint);
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Recipient AP URL is " + sRecipientAPUrl);

    // Is it for us?
    if (sRecipientAPUrl == null || !sRecipientAPUrl.contains (sOwnAPUrl))
    {
      final String sErrorMsg = sMessageID +
                               " Internal error: The request is targeted for '" +
                               sRecipientAPUrl +
                               "' and is not for us (" +
                               sOwnAPUrl +
                               ")";
      s_aLogger.error (sErrorMsg);
      throw new OpenAS2Exception (sErrorMsg);
    }
  }

  private static void _checkIfEndpointCertificateMatches (@Nonnull final EndpointType aRecipientEndpoint,
                                                          @Nonnull final String sMessageID) throws OpenAS2Exception
  {
    final X509Certificate aOurCert = AS2PeppolServletConfiguration.getAPCertificate ();
    if (aOurCert == null)
      throw new OpenAS2Exception ("The certificate of this AP is not configured!");

    final String sRecipientCertString = aRecipientEndpoint.getCertificate ();
    X509Certificate aRecipientCert = null;
    try
    {
      aRecipientCert = CertificateHelper.convertStringToCertficate (sRecipientCertString);
    }
    catch (final CertificateException t)
    {
      throw new OpenAS2Exception (sMessageID +
                                  " Internal error: Failed to convert looked up endpoint certificate string '" +
                                  sRecipientCertString +
                                  "' to an X.509 certificate!",
                                  t);
    }

    if (aRecipientCert == null)
    {
      // No certificate found - most likely because of invalid SMP entry
      throw new OpenAS2Exception (sMessageID +
                                  " No certificate found in looked up endpoint! Is this AP maybe NOT contained in an SMP?");
    }

    // Certificate found
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " Conformant recipient certificate present: " + aRecipientCert.toString ());

    // Compare serial numbers
    if (!aOurCert.getSerialNumber ().equals (aRecipientCert.getSerialNumber ()))
    {
      final String sErrorMsg = sMessageID +
                               " Certificate retrieved from SMP lookup (" +
                               aRecipientCert +
                               ") does not match this APs configured Certificate (" +
                               aOurCert +
                               ") - different serial numbers - ignoring document";
      s_aLogger.error (sErrorMsg);
      throw new OpenAS2Exception (sErrorMsg);
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (sMessageID + " The certificate of the SMP lookup matches our certificate");
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

      if (AS2PeppolServletConfiguration.isReceiverCheckEnabled ())
      {
        final PeppolSBDHDocument aDD = new PeppolSBDHDocumentReader ().extractData (aSBD);
        final String sMessageID = aDD.getInstanceIdentifier ();

        // Get the endpoint information required from the recipient
        final EndpointType aReceiverEndpoint = _getReceiverEndpoint (aDD.getReceiverAsIdentifier (),
                                                                     aDD.getDocumentTypeAsIdentifier (),
                                                                     aDD.getProcessAsIdentifier (),
                                                                     sMessageID);

        if (aReceiverEndpoint == null)
        {
          throw new OpenAS2Exception (sMessageID +
                                      " Failed to resolve endpoint for provided receiver/documentType/process - not handling document");
        }
        // Check if the message is for us
        _checkIfReceiverEndpointURLMatches (aReceiverEndpoint, sMessageID);

        // Get the recipient certificate from the SMP
        _checkIfEndpointCertificateMatches (aReceiverEndpoint, sMessageID);
      }
      else
      {
        s_aLogger.info ("Endpoint checks for the AS2 AP are disabled");
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
