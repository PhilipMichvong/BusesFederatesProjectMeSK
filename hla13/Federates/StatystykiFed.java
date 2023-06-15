/*
 *   Copyright 2007 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL)
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package hla13.hla13.Federates;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import hla13.hla13.domain.Pasazer;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

public class StatystykiFed
{
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private StatystykiFedAmbassador fedamb;

    private void log( String message )
    {
        System.out.println( "StatystykiFederate: " + message );
    }

    private void waitForUser()
    {
        log( "-------------------> Press Enter to Continue <-------------------" );
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

    private LogicalTime convertTime( double time )
    {
        return new DoubleTime( time );
    }

    private LogicalTimeInterval convertInterval( double time )
    {
        return new DoubleTimeInterval( time );
    }
    public void
    runFederate( String federateName ) throws RTIexception
    {
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
        fedamb = new StatystykiFedAmbassador();
        rtiamb.joinFederationExecution( federateName, "PrzystanekFederation", fedamb );
        log( "Joined Federation as " + federateName );

        fedamb.rtiamb = this.rtiamb;
        publishAndSubscribe();
        log( "Published and Subscribed" );
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();


        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }

        enableTimePolicy();
        log( "Time Policy Enabled" );



        while(fedamb.running){
            System.out.println();
            System.out.println("---------------  " + fedamb.federateTime + "  ----------------");
            while(fedamb.logEvents.size() > 0){
                System.out.println(fedamb.logEvents.remove(0));
            }
            advanceTime(1.0);
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
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }
    private void publishAndSubscribe() throws RTIexception
    {
        int interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Koniec" );
        rtiamb.subscribeInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Log" );
        rtiamb.subscribeInteractionClass( interactionHandle );

    }

    private int registerObject() throws RTIexception
    {
        int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.Autobus" );
        return rtiamb.registerObjectInstance( classHandle );
    }
    private void updateAttributeValues( int objectHandle ) throws RTIexception
    {
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        byte[] aaValue = EncodingHelpers.encodeString( "aa:" + getLbts() );

        int classHandle = rtiamb.getObjectClass( objectHandle );
        int aaHandle = rtiamb.getAttributeHandle( "aa", classHandle );

        attributes.add( aaHandle, aaValue );

        rtiamb.updateAttributeValues( objectHandle,attributes, generateTag() );

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag(), time );
    }

    private void sendInteractionPodejscieDoOkienka(Pasazer pasazer) throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        log(" enter th queue by (id of passenger): " + pasazer.id);

        byte[] nrPasazera = EncodingHelpers.encodeInt(pasazer.id);
        byte[] godzina = EncodingHelpers.encodeInt(pasazer.rozklad.time);

        int classHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.PodejscieDoOkienka" );
        int xaHandle = rtiamb.getParameterHandle( "nrPasazera", classHandle );
        int xbHandle = rtiamb.getParameterHandle( "godzina", classHandle );

        parameters.add( xaHandle, nrPasazera );
        parameters.add( xbHandle, godzina );

        rtiamb.sendInteraction( classHandle, parameters, generateTag() );
    }

    private void sendInteractionWejscieDoAutobusu(Pasazer pasazer) throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        byte[] godzina = EncodingHelpers.encodeInt(pasazer.rozklad.time);

        int classHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.WejscieDoAutobusu" );
        int xbHandle = rtiamb.getParameterHandle( "godzina", classHandle );

        parameters.add( xbHandle, godzina );

        rtiamb.sendInteraction( classHandle, parameters, generateTag() );
    }

    private void sendInteractionKoniec() throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        int classHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Koniec" );

        fedamb.running = false;
        System.out.println("END POINT");
        rtiamb.sendInteraction( classHandle, parameters, generateTag() );
    }

    private void sendInteractionSzukanieTaxi(ArrayList<Integer> doTaxi) throws RTIexception
    {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        StringBuilder builder = new StringBuilder();

        for (Integer pasazerId : doTaxi){
            builder.append(",").append(pasazerId);
        }

        log("------------------ looking for taxi: " + builder);

        builder.deleteCharAt(0);

        byte[] pasazerowieId = EncodingHelpers.encodeString(builder.toString());

        int classHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.SzukanieTaxi" );
        int xbHandle = rtiamb.getParameterHandle( "pasazerowieId", classHandle );

        parameters.add( xbHandle, pasazerowieId );

        rtiamb.sendInteraction( classHandle, parameters, generateTag() );
    }
    private void advanceTime( double timestep ) throws RTIexception
    {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );

        while( fedamb.isAdvancing )
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
        return fedamb.federateTime + fedamb.federateLookahead;
    }

    private byte[] generateTag()
    {
        return (""+System.currentTimeMillis()).getBytes();
    }
    public static void main( String[] args )
    {
        String federateName = "StatystykiFederate";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new PasazerowieFed().runFederate( federateName );
        }
        catch( RTIexception rtie )
        {
            rtie.printStackTrace();
        }
    }
}