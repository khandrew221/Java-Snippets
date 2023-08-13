package CoordConverter;

import AstroModel.AstroModel;
import AstroModel.ModelConsts;
import Maths.LatLong;


public class CoordConverter {
    
    private AstroModel am;
    
    public CoordConverter(AstroModel a) {
        am = a;
    }
    

    /**
     * 
     * @param n     LatLong to be converted
     * @param ind   index of coordinate system to be converted to
     * @return      LatLong converted to the coordinate system signified by ind, or an error LatLong if no conversion could take place 
     */
    public LatLong convertTo(LatLong n, int ind) {
        
        int oSys = n.getCoordSystemIndex();
        
        if (oSys == ModelConsts.GEO_ECLIPTIC_OFDATE || 
            oSys == ModelConsts.HELIO_ECLIPTIC_OFDATE ||
            oSys == ModelConsts.TOPO_ECLIPTIC_OFDATE) {            
            oSys = ModelConsts.ECLIPTIC_OF_DATE;
        }
        
        //no conversion needed, return copy
        if (oSys == ind) {
            return new LatLong(n); 
        }
               
        if (oSys == ModelConsts.EQUATOR_OF_DATE) {
            switch(ind) {
                case ModelConsts.ECLIPTIC_OF_DATE:
                    return EquToEcl(n); 
                case ModelConsts.GALACTIC:
                    return EquToGal(n);       
            }
        } 
        
        if (oSys == ModelConsts.ECLIPTIC_OF_DATE) {
            switch(ind) {
                case ModelConsts.EQUATOR_OF_DATE:
                    return EclToEqu(n);
                case ModelConsts.GALACTIC:
                    return EclToGal(n);                    
                case ModelConsts.HORIZONTAL:
                    return EclToHor(n);
            } 
        } 
               
        
        // Convert to ecliptic of date coords, then to desired coords
        if (oSys != ModelConsts.ECLIPTIC_OF_DATE
            && ind != ModelConsts.ECLIPTIC_OF_DATE) {
            return convertTo(convertTo(n, ModelConsts.ECLIPTIC_OF_DATE), ind);
        }
        
        //conversion failed
        return new LatLong(-1, 0, 0);
    }
    
    /**
     * converts an ecliptic latlong to the equivalent equatorial one
     * @param n     ecliptic LatLong to be converted
     * @return      equivalent equatorial latlong
     */    
    public LatLong EclToEqu(LatLong n) {
        double e = am.getObl();
        double l = n.getLong();
        double b = n.getLat();
        double d = Math.asin(Math.sin(b)*Math.cos(e) + Math.cos(b)*Math.sin(e)*Math.sin(l));
        double a = Math.atan2(Math.cos(b)*Math.cos(e)*Math.sin(l)-Math.sin(b)*Math.sin(e),Math.cos(b)*Math.cos(l));
        return new LatLong(ModelConsts.EQUATOR_OF_DATE, d, a);
    }

    /**
     * converts an equatorial latlong to the equivalent ecliptic one
     * @param n     equatorial LatLong to be converted
     * @return      equivalent ecliptic latlong
     */      
    public LatLong EquToEcl(LatLong n) {
        double e = am.getObl();
        double a = n.getLong();
        double d = n.getLat();
        double b = Math.asin(Math.sin(d)*Math.cos(e) - Math.cos(d)*Math.sin(e)*Math.sin(a));
        double l = Math.atan2(Math.cos(d)*Math.cos(e)*Math.sin(a)+Math.sin(d)*Math.sin(e),Math.cos(d)*Math.cos(a));
        return new LatLong(ModelConsts.ECLIPTIC_OF_DATE, b, l);
    }  
    
    
    
    public LatLong EquToGal(LatLong n) {   
        return sphericalConvert(n, am.GNPOfYear(am.getYear(), false),  am.GCOfYear(am.getYear(), false), ModelConsts.GALACTIC); 
    }
    
    public LatLong EclToGal(LatLong n) { 
        return sphericalConvert(n, am.GNPOfYear(am.getYear(), true),  am.GCOfYear(am.getYear(), true), ModelConsts.GALACTIC); 
    } 
    
    public LatLong EclToHor(LatLong n) { 
        return sphericalConvert(n, am.getGreatCircle("LocalHor").getNP(), am.getGreatCircle("LocalHor").getP0(), ModelConsts.HORIZONTAL); 
    }    

    
    
    /**
     * Conversion to a coordinate system with a known north pole and east point (0 long) location.  
     * All inputs must be in the same coordinate system
     * @param n
     * @param NP
     * @param EP
     * @param coords
     * @return 
     */
    public LatLong sphericalConvert(LatLong n, LatLong NP, LatLong EP, int coords) {
        
        double ap = NP.getLong();
        double dp = NP.getLat();
        double ae = EP.getLong();
        double de = EP.getLat();
        double d = n.getLat();
        double a = n.getLong();
        
        double BK = Math.acos(Math.sin(de)*Math.cos(dp)-Math.cos(de)*Math.sin(dp)*Math.sin(ap-ae));
        double long0 = Math.atan2(Math.cos(de)*Math.sin(ae-ap), Math.cos(dp)*Math.sin(de)-Math.sin(dp)*Math.cos(de)*Math.cos(ae-ap));       
        double b = Math.asin(Math.sin(dp)*Math.sin(d)+Math.cos(dp)*Math.cos(d)*Math.cos(a-ap));
        double l = Math.atan2(Math.cos(d)*Math.sin(a-ap), Math.cos(dp)*Math.sin(d)-Math.sin(dp)*Math.cos(d)*Math.cos(a-ap));
        
        //adjustment to make sure EP is at 0 longitude.  
        //!!! why is this necessary????
        double adj = 0;
        if (BK - long0 > 0) {
            adj = Math.PI*2 - (BK-long0);
        } else {
            adj = 0 - (BK-long0);
        }
       
        return new LatLong(coords, b, BK-l+adj);
    }  
    
    public LatLong latRot(LatLong n, double e) {
        double l = n.getLong();
        double b = n.getLat();
        double d = Math.asin(Math.sin(b)*Math.cos(e) + Math.cos(b)*Math.sin(e)*Math.sin(l));
        double a = Math.atan2(Math.cos(b)*Math.cos(e)*Math.sin(l)-Math.sin(b)*Math.sin(e),Math.cos(b)*Math.cos(l));
        return new LatLong(0, d, a);
    }  
