package com.helger.as2servlet.sbd;

import javax.annotation.Nonnull;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI;
import com.helger.commons.annotations.IsSPIImplementation;

@IsSPIImplementation
public class MockIncomingSBDHandler implements IAS2IncomingSBDHandlerSPI
{
  public void handleIncomingSBD (@Nonnull final StandardBusinessDocument aSBD) throws Exception
  {
    // Do something with the incoming SBD
    System.out.println (aSBD.toString ());
  }
}
