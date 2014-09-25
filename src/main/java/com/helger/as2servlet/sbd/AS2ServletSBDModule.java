/**
 * Copyright (C) 2014 Philip Helger (www.helger.com)
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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import com.helger.commons.lang.ServiceLoaderUtils;

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
      s_aLogger.warn ("No SPI handler of type " +
                      IAS2IncomingSBDHandlerSPI.class.getName () +
                      " for incoming SBD documents is registered. Therefore incoming documents will NOT be handled!");
  }

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    // Using the store action, because this action is automatically called upon
    // receipt
    return IProcessorStorageModule.DO_STORE.equals (sAction) && aMsg instanceof AS2Message;
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
