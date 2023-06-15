package hla13.hla13;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

public class TaxiFed {
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private TaxiFedAmbassador taxiFedAmbassador;

    private int liczbaWolnych = 5;

    private void log( String message )
    {
        System.out.println( "TaxiFederate    : " + message );
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
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    private LogicalTimeInterval convertInterval(double time )
    {
        // PORTICO SPECIFIC!!
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
        taxiFedAmbassador = new TaxiFedAmbassador();
        rtiamb.joinFederationExecution(federateName, "PrzystanekFederation", taxiFedAmbassador);
        log("Joined Federation as " + federateName);
        taxiFedAmbassador.rtiamb = this.rtiamb;
        publishAndSubscribe();
        log( "Published and Subscribed" );
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( taxiFedAmbassador.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( taxiFedAmbassador.isReadyToRun == false )
        {
            rtiamb.tick();
        }
        enableTimePolicy();
        log( "Time Policy Enabled" );
        //int objectHandle = registerObject();
        //log( "Registered Object, handle=" + objectHandle );

        ///TODO main for loop
        Random random = new Random();
        int czasDodaniaTaxi = random.nextInt(2)+1;
        while(taxiFedAmbassador.running){
            System.out.println();
            log("" + taxiFedAmbassador.federateTime);

            ArrayList<String> weszliDoTaxi = new ArrayList<>();
            if (taxiFedAmbassador.pasazerowieId.size() > 0){
                while(liczbaWolnych > 0 && taxiFedAmbassador.pasazerowieId.size() > 0){
                    --liczbaWolnych;
                    weszliDoTaxi.add(taxiFedAmbassador.pasazerowieId.remove(0));
                }
                for (String id : weszliDoTaxi){
                    System.out.print(id + ", ");
                }
            }

            if (weszliDoTaxi.size() > 0) {
                sendInteractionWejscieDoTaxi(weszliDoTaxi);
            }

            if (liczbaWolnych < 5){
                if (czasDodaniaTaxi == 0){
                    ++liczbaWolnych;
                    czasDodaniaTaxi = random.nextInt(2)+1;
                }else {
                    --czasDodaniaTaxi;
                }
            }

            if (taxiFedAmbassador.running){
                advanceTime(1.0);
            }
        }

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
        LogicalTime currentTime = convertTime( taxiFedAmbassador.federateTime );
        LogicalTimeInterval lookahead = convertInterval( taxiFedAmbassador.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( taxiFedAmbassador.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( taxiFedAmbassador.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception
    {
        int interactionClassHandle = rtiamb.getInteractionClassHandle("InteractionRoot.SzukanieTaxi");
        rtiamb.subscribeInteractionClass(interactionClassHandle);

        interactionClassHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WejscieDoTaxi");
        rtiamb.publishInteractionClass(interactionClassHandle);

        interactionClassHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Koniec");
        rtiamb.subscribeInteractionClass(interactionClassHandle);
    }
    private int registerObject() throws RTIexception
    {
        int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.Autobus" );
        return rtiamb.registerObjectInstance( classHandle );
    }

    private void updateAttributeValues( int objectHandle ) throws RTIexception
    {

    }

    private void sendInteractionWejscieDoTaxi(ArrayList<String> pasazerowieId) throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        StringBuilder builder1 = new StringBuilder();

        for (String pasazerId : pasazerowieId){
            builder1.append(",").append(pasazerId);
        }

        builder1.deleteCharAt(0);

        byte[] pasazerowieOdjechali = EncodingHelpers.encodeString(builder1.toString());

        int classHandle = rtiamb.getInteractionClassHandle("InteractionRoot.WejscieDoTaxi");
        int xaHandle = rtiamb.getParameterHandle("pasazerowieId", classHandle);

        parameters.add(xaHandle, pasazerowieOdjechali);

        System.out.println("W taxi : " + builder1);

        rtiamb.sendInteraction(classHandle, parameters, generateTag());
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        // request the advance
        taxiFedAmbassador.isAdvancing = true;
        LogicalTime newTime = convertTime( taxiFedAmbassador.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( taxiFedAmbassador.isAdvancing )
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
        return taxiFedAmbassador.federateTime + taxiFedAmbassador.federateLookahead;
    }
    private byte[] generateTag()
    {
        return (""+System.currentTimeMillis()).getBytes();
    }

    public static void main( String[] args )
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "TaxiFed";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new TaxiFed().runFederate( federateName );
        }
        catch( RTIexception rtie )
        {
            rtie.printStackTrace();
        }
    }

}
