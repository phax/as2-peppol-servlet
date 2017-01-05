/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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
package com.helger.peppol.as2servlet.mock;

import javax.annotation.Nonnull;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.peppol.as2servlet.IAS2IncomingSBDHandlerSPI;

@IsSPIImplementation
public class MockIncomingSBDHandler implements IAS2IncomingSBDHandlerSPI
{
  public void handleIncomingSBD (@Nonnull final StandardBusinessDocument aSBD) throws Exception
  {
    // Do something with the incoming SBD
    System.out.println (aSBD.toString ());
  }
}
