package com.helger.as2servlet;

import java.security.cert.X509Certificate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.peppol.smpclient.SMPClientReadonly;

@NotThreadSafe
public final class AS2PeppolServletConfiguration
{
  public static final boolean DEFAULT_RECEIVER_CHECK_ENABLED = false;

  private static boolean s_bReceiverCheckEnabled = DEFAULT_RECEIVER_CHECK_ENABLED;
  private static SMPClientReadonly s_aSMPClient;
  private static String s_sAS2EndpointURL;
  private static X509Certificate s_aAPCertificate;

  private AS2PeppolServletConfiguration ()
  {}

  /**
   * @return <code>true</code> if the checks for endpoint URL and endpoint
   *         certificate are enabled, <code>false</code> otherwise. By default
   *         the checks are disabled for backwards compatibility.
   */
  public static boolean isReceiverCheckEnabled ()
  {
    return s_bReceiverCheckEnabled;
  }

  public static void setReceiverCheckEnabled (final boolean bReceiverCheckEnabled)
  {
    s_bReceiverCheckEnabled = bReceiverCheckEnabled;
  }

  /**
   * @return The SMP client object that should be used for the SMP lookup. It is
   *         customizable because it depends either on the SML or a direct URL
   *         to the SMP may be provided. May be <code>null</code> if not yet
   *         configured.
   */
  @Nullable
  public static SMPClientReadonly getSMPClient ()
  {
    return s_aSMPClient;
  }

  public static void setSMPClient (@Nullable final SMPClientReadonly aSMPClient)
  {
    s_aSMPClient = aSMPClient;
  }

  /**
   * @return The URL of this AP to compare to against the SMP lookup result upon
   *         retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static String getAS2EndpointURL ()
  {
    return s_sAS2EndpointURL;
  }

  public static void setAS2EndpointURL (@Nullable final String sAS2EndpointURL)
  {
    s_sAS2EndpointURL = sAS2EndpointURL;
  }

  /**
   * @return The certificate of this AP to compare to against the SMP lookup
   *         result upon retrieval. Is <code>null</code> by default.
   */
  @Nullable
  public static X509Certificate getAPCertificate ()
  {
    return s_aAPCertificate;
  }

  public static void setAPCertificate (@Nullable final X509Certificate aAPCertificate)
  {
    s_aAPCertificate = aAPCertificate;
  }
}
