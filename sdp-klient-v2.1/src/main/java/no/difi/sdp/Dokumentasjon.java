package no.difi.sdp;

import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.digital_post.EpostVarsel;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;
import no.difi.sdp.client2.domain.digital_post.SmsVarsel;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.fysisk_post.*;
import no.difi.sdp.client2.domain.kvittering.*;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class Dokumentasjon {

    public static void digitalPost() {
        Sertifikat mottakerSertifikat = null; //Mottakers sertifikat fra Oppslagstjenesten
        String orgnrPostkasse = "123456789";

        Mottaker mottaker = Mottaker.builder("99999999999",
                "ola.nordmann#2222", mottakerSertifikat, orgnrPostkasse).build();


        SmsVarsel smsVarsel = SmsVarsel.builder("4799999999",
                "Du har mottatt brev i din digitale postkasse")
                .build();

        EpostVarsel epostVarsel = EpostVarsel.builder("example@fiktivepost.no.",
                "Du har mottatt brev i din digitale postkasse")
                .varselEtterDager(asList(1, 4, 10))
                .build();

        DigitalPost digitalPost = DigitalPost.builder(mottaker, "Ikke-sensitiv tittel")
                .virkningsdato(new Date())
                .aapningskvittering(false)
                .sikkerhetsnivaa(Sikkerhetsnivaa.NIVAA_3)
                .epostVarsel(epostVarsel)
                .smsVarsel(smsVarsel)
                .build();

    }

    public static void fysiskPost() {
        Sertifikat utskriftsleverandørSertifikat = null;    //Printsertifikat fra Oppslagstjenesten
        TekniskMottaker utskriftsleverandør = new TekniskMottaker("99999999", utskriftsleverandørSertifikat);

        FysiskPost fysiskPost = FysiskPost.builder()
                .adresse(
                        KonvoluttAdresse.build("Ola Nordmann")
                                .iNorge("Fjellheimen 22", "", "", "0001", "Oslo")
                                .build())
                .retur(
                        Returhaandtering.DIREKTE_RETUR.MAKULERING_MED_MELDING,
                        KonvoluttAdresse.build("Returkongen")
                                .iNorge("Returveien 3", "", "", "0002", "Oslo")
                                .build())
                .sendesMed(Posttype.A_PRIORITERT)
                .utskrift(Utskriftsfarge.FARGE, utskriftsleverandør)
                .build();
    }

    public static void opprettForsendelse() {
        DigitalPost digitalPost = null; //Som initiert tidligere

        Dokument hovedDokument = Dokument
                .builder("Sensitiv brevtittel", new File("/sti/til/dokument"))
                .mimeType("application/pdf")
                .build();

        Dokumentpakke dokumentpakke = Dokumentpakke.builder(hovedDokument)
                .vedlegg(new ArrayList<Dokument>())
                .build();

        Behandlingsansvarlig behandlingsansvarlig =
                Behandlingsansvarlig
                        .builder("999999999")
                        .build();

        Forsendelse forsendelse = Forsendelse
                .digital(behandlingsansvarlig, digitalPost, dokumentpakke)
                .konversasjonsId(UUID.randomUUID().toString())
                .prioritet(Prioritet.NORMAL)
                .mpcId("KøId")
                .spraakkode("NO")
                .build();
    }

    public static void OppretteKlientOgSendPost() {
        Forsendelse forsendelse = null;         //Som initiert tidligere
        KeyStore virksomhetssertifikat = null;  //Last inn sertifikat her.

        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder()
                .meldingsformidlerRoot("https://qaoffentlig.meldingsformidler.digipost.no/api/ebms")
                .connectionTimeout(20, TimeUnit.SECONDS)
                .build();

        TekniskAvsender avsender = TekniskAvsender
                .builder("000000000",
                Noekkelpar.fraKeyStoreUtenTrustStore(
                        virksomhetssertifikat,
                        "sertifikatAlias",
                        "sertifikatPassord"))
                .build();

        SikkerDigitalPostKlient sikkerDigitalPostKlient = new SikkerDigitalPostKlient(avsender, klientKonfigurasjon);

        try {
            sikkerDigitalPostKlient.send(forsendelse);
        } catch (SendException sendException) {
            SendException.AntattSkyldig antattSkyldig = sendException.getAntattSkyldig();
            String message = sendException.getMessage();
        }
    }

    public static void HentKvitteringOgBekreft() {
        SikkerDigitalPostKlient sikkerDigitalPostKlient = null;     //Som initiert tidligere

        KvitteringForespoersel kvitteringForespoersel = KvitteringForespoersel.builder(Prioritet.NORMAL).mpcId("KøId").build();
        ForretningsKvittering forretningsKvittering = sikkerDigitalPostKlient.hentKvittering(kvitteringForespoersel);

        if (forretningsKvittering instanceof LeveringsKvittering) {
            //Forsendelse er levert til digital postkasse
        } else if (forretningsKvittering instanceof AapningsKvittering) {
            //Forsendelse ble åpnet av mottaker
        } else if (forretningsKvittering instanceof MottaksKvittering) {
            //Kvittering på sending av fysisk post
        } else if (forretningsKvittering instanceof ReturpostKvittering) {
            //Forsendelse er blitt sendt i retur
        } else if (forretningsKvittering instanceof Feil) {
            //Feil skjedde under sending
        }

        sikkerDigitalPostKlient.bekreft(forretningsKvittering);
    }
}
