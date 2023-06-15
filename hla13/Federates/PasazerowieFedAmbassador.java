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

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import hla13.hla13.Events.ObsluzenieKlientaEvent;
import hla13.hla13.Events.OdjazdEvent;
import hla13.hla13.Events.RozkladEvent;
import hla13.hla13.domain.Rozklad;
import org.portico.impl.hla13.types.DoubleTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class PasazerowieFedAmbassador extends NullFederateAmbassador
{
    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;
    protected boolean running            = true;
    protected boolean scheduleReady = false;

    public RTIambassador rtiamb;

    public ArrayList<RozkladEvent> rozkladEvents = new ArrayList<RozkladEvent>();
    public ArrayList<OdjazdEvent> odjazdEvents = new ArrayList<OdjazdEvent>();
    public ArrayList<ObsluzenieKlientaEvent> obsluzenieKlientaEvents = new ArrayList<ObsluzenieKlientaEvent>();

    public ArrayList<String> weszliDoTaxi = new ArrayList<>();

    public PasazerowieFedAmbassador()
    {
    }

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println("----------------------------------------------");
        System.out.println( "FederateAmbassador: " + message );
        System.out.println("----------------------------------------------");
    }

    public void synchronizationPointRegistrationFailed( String label )
    {
        log( "Failed to register sync point: " + label);
    }

    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(Example13Federate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(Example13Federate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }
    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }

    public void discoverObjectInstance( int theObject,
                                        int theObjectClass,
                                        String objectName )
    {
        log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName );
    }

    public void reflectAttributeValues( int theObject,
                                        ReflectedAttributes theAttributes,
                                        byte[] tag ){
    String className;
        try {
            className = rtiamb.getObjectClassName(theObject);
        } catch (ObjectClassNotDefined | FederateNotExecutionMember | RTIinternalError e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag )
    {
        receiveInteraction( interactionClass, theInteraction, tag, null, null );
    }

    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag,
                                    LogicalTime theTime,
                                    EventRetractionHandle eventRetractionHandle )
    {

        try {
            String className = rtiamb.getInteractionClassName(interactionClass);

            if (Objects.equals(className, "InteractionRoot.Rozklad")){
                try {
                    ArrayList<String> godziny = new ArrayList<>(
                            Arrays.asList(
                                    EncodingHelpers.decodeString(
                                            theInteraction.getValue(0)
                                    ).split(",")
                            )
                    );
                    ArrayList<String> linie = new ArrayList<>(
                            Arrays.asList(
                                    EncodingHelpers.decodeString(
                                            theInteraction.getValue(1)
                                    ).split(",")
                            )
                    );
                    ArrayList<Rozklad> rozklads = new ArrayList<>();
                    for (int i = 0; i < godziny.size(); i++){
                        rozklads.add(
                                new Rozklad(
                                        Integer.parseInt(godziny.get(i)),
                                        Integer.parseInt(linie.get(i))
                                )
                        );
                    }
                    rozkladEvents.add(new RozkladEvent(rozklads));
                    scheduleReady = true;
                } catch (ArrayIndexOutOfBounds e) {
                    throw new RuntimeException(e);
                }
            }
            if (Objects.equals(className, "InteractionRoot.WejscieDoTaxi")){
                weszliDoTaxi = new ArrayList<>(
                        Arrays.asList(
                                EncodingHelpers.decodeString(
                                        theInteraction.getValue(0)
                                ).split(",")
                        )
                );
            }
            if (Objects.equals(className, "InteractionRoot.Odjazd")){
                this.odjazdEvents.add(
                        new OdjazdEvent(
                            EncodingHelpers.decodeInt(theInteraction.getValue(0)),
                            EncodingHelpers.decodeInt(theInteraction.getValue(1))
                        )
                );
            }
            if (Objects.equals(className, "InteractionRoot.ObsluzenieKlienta")){
                this.obsluzenieKlientaEvents.add(
                        new ObsluzenieKlientaEvent(
                                EncodingHelpers.decodeInt(theInteraction.getValue(0))
                        )
                );
            }
        } catch (InteractionClassNotDefined | FederateNotExecutionMember | RTIinternalError e) {
            throw new RuntimeException(e);
        } catch (ArrayIndexOutOfBounds e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObjectInstance( int theObject, byte[] userSuppliedTag )
    {
        log( "Object Removed: handle=" + theObject );
    }

    public void removeObjectInstance( int theObject,
                                      byte[] userSuppliedTag,
                                      LogicalTime theTime,
                                      EventRetractionHandle retractionHandle )
    {
        log( "Object Removed: handle=" + theObject );
    }


    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
}
