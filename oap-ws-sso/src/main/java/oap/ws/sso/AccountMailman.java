/*
 * Copyright (c) Xenoss
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package oap.ws.sso;

import lombok.extern.slf4j.Slf4j;
import oap.mail.MailAddress;
import oap.mail.Mailman;
import oap.mail.Message;
import oap.mail.Template;
import oap.ws.sso.model.UserData;

import javax.annotation.Nonnull;

@Slf4j
public class AccountMailman {
    private final Mailman mailman;
    private final Accounts accounts;
    private final String fromPersonal;
    private final String fromEmail;
    private final String confirmUrl;

    public AccountMailman( @Nonnull Mailman mailman, @Nonnull Accounts accounts, @Nonnull String fromPersonal, @Nonnull String fromEmail, @Nonnull String confirmUrl ) {
        this.mailman = mailman;
        this.accounts = accounts;
        this.fromPersonal = fromPersonal;
        this.fromEmail = fromEmail;
        this.confirmUrl = confirmUrl;
    }

    public void sendInvitedEmail( @Nonnull UserData user ) {
        sendUserCreatedEmail( user, "user-invited" );
    }

    public void sendRegisteredEmail( @Nonnull UserData user ) {
        sendUserCreatedEmail( user, "user-registered" );
    }

    private void sendUserCreatedEmail( @Nonnull UserData user, @Nonnull String xmail ) {
        Template template = Template.of( "/io/xenoss/account/mail/" + xmail )
            .orElseGet( () -> Template.of( "/io/xenoss/account/mail/" + xmail + ".default" ).orElseThrow() );
        template.bind( "user", user.user );
        template.bind( "confirmUrl", confirmUrl( user ) );
        Message message = template.buildMessage();
        message.from = MailAddress.of( fromPersonal, fromEmail );
        message.to.add( MailAddress.of( user.user.firstName + " " + user.user.lastName, user.user.email ) );
        log.trace( "sending mail {}", message );
        mailman.send( message );
    }

    public String confirmUrl( UserData user ) {
        return confirmUrl + "/users/confirm/" + user.user.email + "?accessKey=" + user.getAccessKey() + "&apiKey=" + user.user.apiKey;
    }
}
