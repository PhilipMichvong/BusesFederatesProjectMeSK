package hla13.hla13.Federates;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import hla13.hla13.Events.WejscieDoAutobusuEvent;
import hla13.hla13.domain.Autobus;
import hla13.hla13.domain.Rozklad;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

public class AutobusyFed {
    public static final String READY_TO_RUN = "ReadyToRun";
    public static final String INITIALIZED = "initialized";
    private RTIambassador rtiamb;
    private AutobusyFedAmbassador autobusyFedAmbassador;

    private Autobus[] autobusy = {
            new Autobus(1, new Rozklad(100,1 ), 8),
            new Autobus(2, new Rozklad(150,1), 8),
            new Autobus(3, new Rozklad(260,1), 8),
            new Autobus(4, new Rozklad(125, 2), 8),
            new Autobus(5, new Rozklad(175,2), 8),
            new Autobus(6, new Rozklad(250,2), 8),
            new Autobus(7, new Rozklad(140, 3),8),
            new Autobus(8, new Rozklad(200, 3), 8),
            new Autobus(9, new Rozklad(270,3), 8),
            new Autobus(10,new Rozklad(180, 4), 8),
            new Autobus(11,new Rozklad(280,4), 8),
            new Autobus(12,new Rozklad(295,4), 8),
    };

    private void log( String message )
    {
        System.out.println( "AutobusyFederate    : " + message );
    }

    private void waitForUser()
    {
        log( " -------------------> Press Enter to Continue <-------------------" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private LogicalTime convertTime(double time )
    {
        return new DoubleTime( time );
    }

    private LogicalTimeInterval convertInterval(double time )
    {
        return new DoubleTimeInterval( time );
    }

    public void runFederate(String federateName) throws RTIexception{
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
        try
        {
            File fom = new File( "testfom.fed" );
            rtiamb.createFederationExecution( "PrzystanekFederation",
                    fom.toURI().toURL() );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }
        autobusyFedAmbassador = new AutobusyFedAmbassador();
        rtiamb.joinFederationExecution(federateName, "PrzystanekFederation", autobusyFedAmbassador);
        log("Joined Federation as " + federateName);
        autobusyFedAmbassador.rtiamb = this.rtiamb;
        publishAndSubscribe();
        log( "Published and Subscribed" );
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( autobusyFedAmbassador.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( autobusyFedAmbassador.isReadyToRun == false )
        {
            rtiamb.tick();
        }
        enableTimePolicy();
        log( "Time Policy Enabled" );

        sendInteractionRozklad();
        while(autobusyFedAmbassador.running){
            System.out.println();
            log("" + autobusyFedAmbassador.federateTime);

            if(autobusyFedAmbassador.wejscieDoAutobusuEvents.size() > 0 ){
                WejscieDoAutobusuEvent event = autobusyFedAmbassador.wejscieDoAutobusuEvents.remove(0);
                for (Autobus autobus: autobusy){
                    if (autobus.getSchedule().time == event.getTime()){
                        autobus.setEmptyPlaces(autobus.getEmptyPlaces()-1);
                        log("Autobus :" + autobus.schedule.line + " " + autobus.schedule.time + " number of places: " + autobus.getEmptyPlaces());
                        if (autobus.getEmptyPlaces()==0){
                            log("Autobus " + autobus.schedule.line + " " + autobus.schedule.time + " left full");
                            sendInteractionOdjazd(autobus);
                        }
                    }
                }
            }

            for (Autobus autobus: autobusy){
                if (autobus.getSchedule().time == autobusyFedAmbassador.federateTime){
                    if (autobus.getEmptyPlaces()!=0){
                        log("Autobus " + autobus.schedule.line + " " + autobus.schedule.time + " left with " + autobus.emptyPlaces + " empty places");
                        sendInteractionOdjazd(autobus);
                    }
                }
            }

            advanceTime(1.0);
        }

        for (Autobus autobus : autobusy){
            System.out.println("Autobus " + autobus.schedule.line + " " + autobus.schedule.time + " number of empty places: " + autobus.emptyPlaces);
        }
        //deleteObject( objectHandle );
        //log( "Deleted Object, handle=" + objectHandle );

        rtiamb.resignFederationExecution( ResignAction.NO_ACTION );
        log( "Resigned from Federation" );

        try
        {
            rtiamb.destroyFederationExecution( "PrzystanekFederation" );
            log( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( autobusyFedAmbassador.federateTime );
        LogicalTimeInterval lookahead = convertInterval( autobusyFedAmbassador.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( autobusyFedAmbassador.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( autobusyFedAmbassador.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception
    {
        int RozkladHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Rozklad" );
        rtiamb.publishInteractionClass(RozkladHandle);
        int OdjazdHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Odjazd" );
        rtiamb.publishInteractionClass(OdjazdHandle);
        int WejscieDoAutobusuHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WejscieDoAutobusu");
        rtiamb.subscribeInteractionClass(WejscieDoAutobusuHandle);
    }
    private int registerObject() throws RTIexception
    {
        int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.Autobus" );
        return rtiamb.registerObjectInstance( classHandle );
    }

    private void updateAttributeValues( int objectHandle ) throws RTIexception
    {

    }

    private void sendInteractionRozklad() throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        StringBuilder builder1 = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        for (Autobus autobus: autobusy){
            builder1.append(",").append(autobus.getSchedule().time);
            builder2.append(",").append(autobus.getSchedule().line);
        }

        builder1.deleteCharAt(0);
        builder2.deleteCharAt(0);

        byte[] xaValue = EncodingHelpers.encodeString(builder1.toString());
        byte[] xbValue = EncodingHelpers.encodeString(builder2.toString());

        int classHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Rozklad");
        int xaHandle = rtiamb.getParameterHandle("godziny", classHandle);
        int xbHandle = rtiamb.getParameterHandle("linie", classHandle);

        parameters.add(xaHandle, xaValue);
        parameters.add(xbHandle, xbValue);

        System.out.println(builder1);
        System.out.println(builder2);

        rtiamb.sendInteraction(classHandle, parameters, generateTag());
    }

    private void sendInteractionOdjazd(Autobus autobus) throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        byte[] xaValue = EncodingHelpers.encodeInt(autobus.getSchedule().time);
        byte[] xbValue = EncodingHelpers.encodeInt(autobus.getSchedule().line);

        int classHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Odjazd");
        int xaHandle = rtiamb.getParameterHandle("godzina", classHandle);
        int xbHandle = rtiamb.getParameterHandle("linia", classHandle);

        parameters.add(xaHandle, xaValue);
        parameters.add(xbHandle, xbValue);

        log("Leaves " + autobus.getSchedule().line + " at: " + autobus.getSchedule().time);
        rtiamb.sendInteraction(classHandle, parameters, generateTag());
    }

    private void advanceTime( double timestep ) throws RTIexception
    {

        autobusyFedAmbassador.isAdvancing = true;
        LogicalTime newTime = convertTime( autobusyFedAmbassador.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );


        while( autobusyFedAmbassador.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private void deleteObject( int handle ) throws RTIexception
    {
        rtiamb.deleteObjectInstance( handle, generateTag() );
    }

    private double getLbts()
    {
        return autobusyFedAmbassador.federateTime + autobusyFedAmbassador.federateLookahead;
    }
    private byte[] generateTag()
    {
        return (""+System.currentTimeMillis()).getBytes();
    }

    public static void main( String[] args )
    {

        String federateName = "AutobusyFed";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new AutobusyFed().runFederate( federateName );
        }
        catch( RTIexception rtie )
        {
            rtie.printStackTrace();
        }
    }

}
