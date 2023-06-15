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
import java.util.Random;

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

import hla13.hla13.Events.ObsluzenieKlientaEvent;
import hla13.hla13.domain.Pasazer;
import hla13.hla13.domain.Rozklad;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

public class PasazerowieFed
{
    public static final String READY_TO_RUN = "ReadyToRun";
    private RTIambassador rtiamb;
    private PasazerowieFedAmbassador fedamb;

    private ArrayList<Pasazer> pasazerowie = new ArrayList<Pasazer>();
    private ArrayList<Rozklad> rozklad = new ArrayList<Rozklad>();
    private ArrayList<Rozklad> niedostepne = new ArrayList<Rozklad>();

    private String log( String message)
    {
        String logMessage =  "PasazerowieFederate: " + message;
        System.out.println(logMessage);
        return logMessage;
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
        fedamb = new PasazerowieFedAmbassador();
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


        Random random = new Random();
        int passengerAddTime = random.nextInt(2);
        while(fedamb.running){

            if (rozklad.size() == 0){
                if (fedamb.rozkladEvents.size() > 0){
                    rozklad = fedamb.rozkladEvents.remove(0).getSchedule();
                    log("bus schedule received");
                }
            }
            log("" + fedamb.federateTime);
            if (fedamb.obsluzenieKlientaEvents.size() > 0){
                ObsluzenieKlientaEvent event = fedamb.obsluzenieKlientaEvents.remove(0);
                if (event.passengerNum != 0){
                    for (Pasazer pasazer : pasazerowie){
                        if (pasazer.id == event.passengerNum && (pasazer.status == "oczekujeKolejka" || pasazer.status == "okienko") ){
                            pasazer.status = "wAutobusie";
                            log("Passenger " + pasazer.id + " " + pasazer.status);
                            sendInteractionWejscieDoAutobusu(pasazer);
                        }
                    }
                }
            }
            boolean przyOkienku = false;
            for (Pasazer pasazer : pasazerowie){
                if (pasazer.status == "okienko"){
                    przyOkienku = true;
                    break;
                }
            }
            if (!przyOkienku){
                for (Pasazer pasazer : pasazerowie){
                    if (pasazer.status == "oczekujeKolejka"){
                        pasazer.status = "okienko";
                        sendInteractionPodejscieDoOkienka(pasazer);
                        break;
                    }
                }
            }

            if (fedamb.odjazdEvents.size() > 0){
                if (!(niedostepne.contains(fedamb.odjazdEvents.get(0).getSchedule()))) {
                    this.niedostepne.add(fedamb.odjazdEvents.remove(0).getSchedule());
                    log("Left " + niedostepne.get(niedostepne.size() - 1).line + " at time: " + niedostepne.get(niedostepne.size() - 1).time);
                    for (Pasazer pasazer : pasazerowie) {
                        if (pasazer.rozklad.time == niedostepne.get(niedostepne.size() - 1).time
                                && (pasazer.status == "oczekujeKolejka" || pasazer.status == "okienko") ) {
                            System.out.println(pasazer.id + " " + pasazer.rozklad.time + " " + pasazer.status);
                            pasazer.status = "oczekujeTaxi";
                            pasazer.czasZmiany = fedamb.federateTime;
                            log("Passenger " + pasazer.id + " for an hour: " + pasazer.rozklad.time + " " + pasazer.status + " " + pasazer.czasZmiany);
                        }
                    }
                }
            }


            while (fedamb.weszliDoTaxi.size() > 0){
                String id = fedamb.weszliDoTaxi.remove(0);
                for (Pasazer pasazer : pasazerowie){
                    if (pasazer.id == Integer.parseInt(id)){
                        System.out.print("Passenger " + pasazer.id + " " + pasazer.status);

                        pasazer.status = "wTaxi";
                        pasazer.czasZmiany = fedamb.federateTime - pasazer.czasZmiany;
                        System.out.println(" ----->> " + pasazer.status + " it took: " + pasazer.czasZmiany);
                        break;
                    }
                }
            }

            ArrayList<Integer> doTaxi = new ArrayList<Integer>();
            for (Pasazer pasazer : pasazerowie){
                if (pasazer.status == "oczekujeTaxi"){
                    pasazer.status = "wTaxi??";
                    doTaxi.add(pasazer.id);
                }
            }
            if (doTaxi.size() > 0){
                sendInteractionSzukanieTaxi(doTaxi);
            }

            if (passengerAddTime == 0){
                AddPassenger();
                passengerAddTime = random.nextInt(2);
            }else {
                passengerAddTime--;
            }


            advanceTime(1.0);
            boolean koniec = true;

            for (Pasazer pasazer : pasazerowie) {
                if (pasazer.status != "wTaxi" && pasazer.status != "wAutobusie") {
                    koniec = false;
                }
            }

            if (koniec && pasazerowie.size() > 0) {
                sendInteractionKoniec();
            }
        }

        int razem = 0;
        int razemRazem = 0;
        for (Rozklad rozklad1 : rozklad){
            System.out.println(rozklad1.time);
            for (Pasazer pasazer : pasazerowie){
                if (pasazer.rozklad.time == rozklad1.time && pasazer.status == "wAutobusie"){
                    ++razem;
                }
            }
            System.out.println(razem);
            System.out.println();
            razemRazem += razem;
            razem = 0;
        }
        System.out.println("Sum :" + razemRazem);
        int oczekujeTaxi = 0;
        for(Pasazer pasazer: pasazerowie){
            if (pasazer.status == "oczekujeTaxi"){
                ++oczekujeTaxi;
            }
        }
        System.out.println("Oczekuje na Taxi : " + oczekujeTaxi);

        int wTaxi = 0;
        for(Pasazer pasazer: pasazerowie){
            if (pasazer.status == "wTaxi"){
                ++wTaxi;
            }
        }
        System.out.println("w Taxi : " + wTaxi);

        System.out.println("Number of passengers: " + pasazerowie.size());

        System.out.println("Passengers: ");
        for (Pasazer pasazer : pasazerowie){
            System.out.println(pasazer.id + " " + pasazer.status + " " + pasazer.czasZmiany);
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

    private void AddPassenger() {
        Random random = new Random();

        int rozkladNumber;
        int k = 0;
        do {
            if (k > 9){
                System.out.println(rozklad.size() + " ---- " + niedostepne.size());
                k=0;
            }else {
                k++;
            }
            if (rozklad.size() == niedostepne.size() || rozklad.size() == 0){
                log("no more busses :(");
                return;
            }
            rozkladNumber = random.nextInt(rozklad.size());
        }while (niedostepne.contains(rozklad.get(rozkladNumber)));

       this.pasazerowie.add(new Pasazer(
                this.pasazerowie.size()+1,
                rozklad.get(rozkladNumber).time,
                rozklad.get(rozkladNumber).line
        ));
       pasazerowie.get(pasazerowie.size()-1).status = "oczekujeKolejka";
        log("Passenger: " + pasazerowie.get(pasazerowie.size()-1).id+ " line "
        + pasazerowie.get(pasazerowie.size()-1).rozklad.line + " time "
        + pasazerowie.get(pasazerowie.size()-1).rozklad.time + " came to the stop");
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
        int interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Rozklad" );
        rtiamb.subscribeInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Odjazd" );
        rtiamb.subscribeInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.PodejscieDoOkienka" );
        rtiamb.publishInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.ObsluzenieKlienta" );
        rtiamb.subscribeInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.WejscieDoAutobusu" );
        rtiamb.publishInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.WejscieDoTaxi" );
        rtiamb.subscribeInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.SzukanieTaxi" );
        rtiamb.publishInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Koniec" );
        rtiamb.publishInteractionClass( interactionHandle );

        interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Log" );
        rtiamb.publishInteractionClass( interactionHandle );
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

        log(" queue entry via: " + pasazer.id);

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
        String federateName = "PasazerowieFederate";
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