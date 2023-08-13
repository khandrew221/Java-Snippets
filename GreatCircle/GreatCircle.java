package Maths;

import exceptions.AntipodalPointsException;
import exceptions.CoordinateSystemMismatchException;
import exceptions.EqualPointsException;
import exceptions.NullPointException;
import java.util.ArrayList;
import java.util.List;

/**
 *  A great circle on a unit sphere centred on the origin 
 */
public class GreatCircle {
    
    private final int CoordSystemID;
    private final Plane plane;          //plane at origin.  Intersection of sphere at origin defines the great circle
    private final LatLong NP;           //north pole, the normal vector to the plane
    private LatLong P0;           //starting point of measurement around great circle
    //private LatLong P1;           //starting point of measurement around great circle    
    
    /**
     * Creates a GreatCircle object for the unique great circle that intersects two points on the sphere.  
     * The first point with be used as the starting point for measuring around the great circle.  
     * Antipodal points or equal points define infinite possible great circles and will throw an exception.
     * 
     * @param a
     * @param b
     * @throws EqualPointsException
     * @throws NullPointException
     * @throws AntipodalPointsException
     * @throws CoordinateSystemMismatchException 
     */    
    public GreatCircle(LatLong a, LatLong b) throws EqualPointsException, NullPointException, AntipodalPointsException, CoordinateSystemMismatchException  {  
        CoordSystemID = a.getCoordSystemIndex();
        P0 = new LatLong(a, 1); //override original radius
        LatLong P1 = new LatLong(b, 1);
        plane = new Plane(P1,P0).toOrigin();
        NP = new LatLong(plane.getNormal());
    }
       
    //great circle from great circle.  Should not need checks(????)
    public GreatCircle(GreatCircle n) {    
        plane = n.getPlane();
        P0 = n.getP0();
        NP = new LatLong(plane.getNormal());
        CoordSystemID = n.getCoordSystemIndex();
    }    

    /**
     * 
     * @param NP
     * @throws CoordinateSystemMismatchException 
     */
    public GreatCircle(LatLong NP) throws CoordinateSystemMismatchException  {      
        this.NP = new LatLong(NP);
        int ci = NP.getCoordSystemIndex();
        CoordSystemID = ci;        
        //generate a plane on the origin using the vector to the north pole
        Vector3D normal = new Vector3D(NP.getCartesian());
        Point3D origin = new Point3D(ci,0,0,0);
        plane = new Plane(origin, normal);  
 
            //generate vectors perpendicular to the pole vector
            Vector3D arb = new Vector3D(ci,0,1,0);
            Vector3D p1 = normal.getCrossProduct(arb);
            LatLong p = new LatLong(p1);
            this.P0 = new LatLong(p);
    }       
   
    public Plane getPlane() {
        return new Plane(plane);
    }
    
     public LatLong getP0() {
        return new LatLong(P0);
    }   
     
     public LatLong getNP() {
        return new LatLong(NP);
    }       
     

    public LatLong getIntersectionDirection(GreatCircle c) {
        try {
            return plane.getIntersectionDirection(c.getPlane());
        } catch(Exception e) {
            return null;
        }
    }     
     
     public boolean samePlane(GreatCircle n) throws CoordinateSystemMismatchException {
        return plane.samePlane(n.getPlane());
    }      
     
   public int getCoordSystemIndex() { return CoordSystemID; }
   
    /**
     * returns the LatLong at a point p radians along the great circle from point P0  
     * @param p
     * @return 
     */
    public LatLong getPointAtAngle(double p) {
        try {
            LatLong P1 = P1();
            
            Point3D A1 = this.P0.getCartesian();
            Point3D B1 = P1.getCartesian();

            double psi = -this.P0.getAngularDistance(P1);  //needs to be minus to track in the right direction

            Point3D C = new Point3D(
                    this.getCoordSystemIndex(),
                    (B1.getX() - A1.getX()*Math.cos(psi)) / Math.sin(psi),
                    (B1.getY() - A1.getY()*Math.cos(psi)) / Math.sin(psi),
                    (B1.getZ() - A1.getZ()*Math.cos(psi)) / Math.sin(psi)
            );     

            Point3D D = new Point3D(
                    this.getCoordSystemIndex(),
                    A1.getX()*Math.cos(p) + C.getX()*Math.sin(p),
                    A1.getY()*Math.cos(p) + C.getY()*Math.sin(p),
                    A1.getZ()*Math.cos(p) + C.getZ()*Math.sin(p)
            );        
                
            return new LatLong(this.getCoordSystemIndex(), D.getLat(), D.getLon());
            
        } catch (Exception e) {
            e.printStackTrace();
        }        
           return null;
    }   
    
    /**
     * Adjusts the position of P0 by p radians
     * @param p 
     */
    public void adjustP0(double p) {               
        LatLong nP0 = getPointAtAngle(p);
        //System.out.println("Old P0: " + P0);
        //System.out.println("New P0: " + nP0);
        //System.out.println("p: " + Math.toDegrees(p));
        //System.out.println("p diff: " + Math.toDegrees(nP0.getAngularDistance(P0)-p));
        P0 = nP0;
        //System.out.println("New P0: " + this.getP0());      
    }      
    
    /**
     * 
     * @param c 
     * @param swapNodes
     */
    public void adjustP0(GreatCircle c, boolean swapNodes) {    
        try {
                //get the intersection direction.  Note: there will be another at the antipode to this point
                LatLong inter = this.getIntersectionDirection(c);
                
                //try this P0
                 P0 = inter;       
                
                //check that it's the ascending node of the two intersections
                LatLong check = getPointAtAngle(Math.PI/50);
                //if point is further from the NP of the other plane, oops, needs the other one!
                if (c.getNP().getAngularDistance(check) > Math.PI/2 && !swapNodes) {
                    P0 = inter.getAntipode(); 
                } else if (c.getNP().getAngularDistance(check) < Math.PI/2 && swapNodes) {
                    P0 = inter.getAntipode(); 
                }
            } catch(Exception e) {
                e.printStackTrace();
      };
    }       

    /**
     * Produces a point on the great circle perpendicular to both P0 and the Pole
     * Should never throw the error to return null, something has gone seriously wrong if it does
     * @return 
     */
    private LatLong P1() {
        try {
            return new LatLong(new Vector3D(P0.getCartesian()).getCrossProduct(new Vector3D(NP.getCartesian())));
        } catch (Exception e) {
            return null;
        }
    }   
    
    
    /**
     * https://edwilliams.org/avform.htm#Par
     * returns the two places where the great circle crosses the given parallel, or an empty list if no crossings
     * @return 
     */
    public List<Double> parallelCross(double targLat) {
        List<Double> out = new ArrayList<>();
        LatLong P1 = P1();
        double l12 = P0.getLong()-P1.getLong();
        double A = Math.sin(P0.getLat())*Math.cos(P1.getLat())*Math.cos(targLat)*Math.sin(l12);
        double B = Math.sin(P0.getLat())*Math.cos(P1.getLat())*Math.cos(targLat)*Math.cos(l12) - Math.cos(P0.getLat())*Math.sin(P1.getLat())*Math.cos(targLat);
        double C = Math.cos(P0.getLat())*Math.cos(P1.getLat())*Math.sin(targLat)*Math.sin(l12);
        double lon = Math.atan2(B,A);                     // ( atan2(y,x) convention)
        
        if (Math.abs(C) > Math.sqrt(A*A + B*B)) {
            return out;
        } else {
            double dlon = Math.acos(C/Math.sqrt(A*A+B*B));
            
            
            
            double lon3_1=mod(P0.getLong()+dlon+lon+Math.PI, 2*Math.PI)-Math.PI;
            double lon3_2=mod(P0.getLong()-dlon+lon+Math.PI, 2*Math.PI)-Math.PI; 
            out.add(lon3_1);
            out.add(lon3_2);
            return out;
        }
    }     
    
    /**
     * Required for parallelCross(); java modulo operator does not work
     * @param y
     * @param x
     * @return 
     */
    private double mod(double y, double x) {     
        return y - x*Math.floor(y/x);
    }
    
    public void DEBUGPointsOnPlaneCheck(){
        try {
        System.out.println("P0 plane distance:" + this.getPlane().getDistance(P0.getCartesian()));
            } catch(Exception e) {
                e.printStackTrace();
      };
    }

    public void DEBUGParametCheck(){
        try {
            
            for (int n = 0; n < 360; n++) {                 
                LatLong l =  this.getPointAtAngle(Math.toRadians(n));
                if (this.getPlane().getDistance(l.getCartesian()) > 0.0000000001)
                    System.out.println("Plane distance error:" + this.getPlane().getDistance(l.getCartesian()));
            }
                
            } catch(Exception e) {
                e.printStackTrace();
      };
    }
    
}
