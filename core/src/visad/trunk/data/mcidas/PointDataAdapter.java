//
// PointDataAdapter.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2002 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad.data.mcidas;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;
import visad.*;
import visad.data.units.*;
import visad.jmet.MetUnits;

/**
 * A class for adapting the results of an ADDE point data request into a 
 * VisAD Data object.
 *
 * @author  Don Murray, Unidata
 */
public class PointDataAdapter {

  AddePointDataReader reader;
  FieldImpl field = null;
  private boolean debug = false;
  private boolean useAliases = true;

  /**
   * Construct a PointDataAdapter using the adde request passed as a string.
   * This will take the data returned from the request and turn it into
   * VisAD Data objects that can be returned by the getData() call.
   *
   * @param  addePointRequest  - string representing the ADDE request
   * @throws VisADException  bad request, no data available, VisAD error
   * @see #getData()
   */
  public PointDataAdapter(String addePointRequest)
      throws VisADException
  {
      this(addePointRequest, true);
  }

  /**
   * Construct a PointDataAdapter using the adde request passed as a string.
   * This will take the data returned from the request and turn it into
   * VisAD Data objects that can be returned by the getData() call.
   *
   * @param  addePointRequest  - string representing the ADDE request
   * @param  useAliases        - for quantities like Latitude, Longitude,etc
   *                             alias the RealTypes to the original McIDAS
   *                             variable name.
   * @throws VisADException  bad request, no data available, VisAD error
   * @see #getData()
   */
  public PointDataAdapter(String addePointRequest, boolean useAliases)
      throws VisADException
  {
    try
    {
      reader = new AddePointDataReader(addePointRequest);
      debug = addePointRequest.indexOf("debug=true") > 0;
      this.useAliases = useAliases;
    }
    catch (AddeException excp)
    {
      throw new VisADException("Problem accessing data");
    }
    makeField();
  }

  // out of this will either come a FieldImpl, a ObservationDBImpl,
  // or a StationObDBImpl
  private void makeField()
      throws VisADException
  {
    // First, let's make a generic FieldImpl from the data
    // the structure will be index -> parameter tuple
    // get all the stuff from the reader
    int[][] data;
    String[] units;
    String[] params;
    Unit[] defaultUnits;
    int[] scalingFactors;
    try
    {
      data = reader.getData(reader.OB_ORDER);
      units = reader.getUnits();
      params = reader.getParams();
      scalingFactors = reader.getScales();
    }
    catch (AddeException ae)
    {
      throw new VisADException("Error retrieving data info");
    }
             
    //int numObs = data[0].length;
    int numObs = data.length;
    if (numObs == 0)
        throw new VisADException("No data available");
    if (debug) System.out.println("Number of observations = " + numObs);

    RealType domainType = RealType.getRealType("index");
    Integer1DSet domain = new Integer1DSet(domainType, numObs);
      
    // now make range (Tuple) type
    MetUnits unitTranslator = new MetUnits();
    int numParams = params.length;
    if (debug) System.out.println("Number of parameters = " + numParams);
    ScalarType[] types = new ScalarType[numParams];
    defaultUnits = new Unit[numParams];
    boolean noText = true;
    for (int i = 0; i < numParams; i++)
    {
      // get the name
      String name = params[i];

      if (units[i].equalsIgnoreCase("CHAR"))
      {
        noText = false;
        if (debug) {
          System.out.println(params[i] + " has units of CHAR");
        }
        types[i] = TextType.getTextType(params[i]);
      } 
      else
      {
        // make the unit
        Unit unit = null;
        try
        {
           unit = 
               (!name.equalsIgnoreCase("LON") )
                   ? Parser.parse(unitTranslator.makeSymbol(units[i]))
                   : Parser.parse("degrees_west");  // fix McIDAS conv.
        }
        catch (NoSuchUnitException ne) {
           if (debug) 
             System.out.println("Unknown unit: " + units[i] + " for " + name);
           unit = null;
        }
        catch (ParseException pe) { unit = null;}
        defaultUnits[i] = unit;

        if (debug) {
          System.out.println(params[i] + " has units " + unit);
          System.out.println("scaling factor = " + scalingFactors[i]);
        }
        types[i] = getQuantity(params[i], unit);
      }
    }

    TupleType rangeType;
    if (noText)  // all Reals
    {
      RealType[] newTypes = new RealType[types.length];
      for (int i = 0; i < types.length; i++) {
        newTypes[i] = (RealType) types[i];
      }
      rangeType = new RealTupleType(newTypes);
    }
    else // all Texts or mixture of Text and Reals
    {
      rangeType = new TupleType(types);
    }

    // make the field
    FunctionType functionType = new FunctionType(domainType, rangeType);
    /*
    field = (noText) 
            ? new FlatField(functionType, domain)
            : new FieldImpl(functionType, domain); 
            */
    field = new FieldImpl(functionType, domain);


    if (debug) System.out.println("filling in data" );
    long millis = System.currentTimeMillis();
    // now, fill in the data
    for (int i = 0; i < numObs; i++)
    {
      Scalar[] scalars = (noText == true) ? new Real[numParams]
                                          : new Scalar[numParams];
      for (int j = 0; j < numParams; j++)
      {
        if (types[j] instanceof TextType) {
          try
          {
            scalars[j] = 
                new Text( (TextType) types[j], 
                         McIDASUtil.intBitsToString(data[i][j]));
          }
          catch (VisADException ex) {;} // shouldn't happen
        } 
        else
        {
            double value =
                data[i][j] == McIDASUtil.MCMISSING
                  ? Double.NaN
                  : data[i][j]/Math.pow(10.0, 
                      (double) scalingFactors[j] );
            try
            {
              scalars[j] =
                new Real(
                    (RealType) types[j], value, defaultUnits[j]);
            } catch (VisADException excp) {  // units problem
               scalars[j] = new Real((RealType) types[j], value);
            }
        }
      }
      try
      {
        Data sample = (noText == true)
                               ? new RealTuple(
                                   (RealTupleType)rangeType, (Real[]) scalars, null)
                               : new Tuple(rangeType, scalars, false);
        field.setSample(i, sample, false);  // don't make copy
      }
      catch (VisADException e) {e.printStackTrace();} 
      catch (java.rmi.RemoteException e) {;}
    }
    if (debug) {
      System.out.println("data fill took " + 
        (System.currentTimeMillis() - millis) + " ms");
    }
  }

  /**
   * Get the VisAD Data object that represents the output from the
   * request.  
   *
   * @return  requested data.  The format is a FieldImpl of
   *          (obnum -> (tuple of parameters)
   */
  public DataImpl getData()
  {
    return field;
  }

  /**  
   * test with 'java visad.data.mcidas.PointDataAdapter args' 
   * @param args ADDE point data request
   */
  public static void main(String[] args)
      throws Exception
  {
    if (args.length == 0) 
    {
      System.out.println("You must specify an ADDE Point Data URL");
      System.exit(-1);
    }
    try
    {
      PointDataAdapter pda = new PointDataAdapter(args[0]);
      Field data = (Field) pda.getData();
      //System.out.println(data.getType());
      visad.python.JPythonMethods.dumpTypes(data);
      /*
      int length = data.getDomainSet().getLength() - 1;
      System.out.println(
              "Sample "+ length + " = " + data.getSample(length));
      */
    }
    catch (VisADException ve)
    {
      System.out.println("Error reading data");
    }
  }

  /**
   * First cut at a standard quantities database.
   */
  private RealType getQuantity(String name, Unit unit) 
    throws VisADException
  {
    RealType type = null;
    if (name.equalsIgnoreCase("lat")) {
      type = RealType.Latitude;
    } else if (name.equalsIgnoreCase("lon")) {
      type = RealType.Longitude;
    } else if (name.equalsIgnoreCase("z") ||
               name.equalsIgnoreCase("zs") ) {
      type = RealType.Altitude;
    } else {
      type = RealType.getRealType(name, unit);
      if (type == null) {
        System.err.println("Problem creating RealType with name " +
                        name + " and unit " + unit);
        type = RealType.getRealTypeByName(name);
        if (type == null) {  // Still a problem
           throw new VisADException(
              "getQuantity(): Couldn't create RealType for " + name);
        }
        System.err.println("Using RealType with name " + name);
      }
    }
    if (RealType.getRealTypeByName(name) == null && useAliases) {
        type.alias(name);
    } else if (!RealType.getRealTypeByName(name).equals(type)) { // alias used
        throw new VisADException(
          "getQuanity(): Two different variables can't have the same alias");
    }
    return type;
  }
}
