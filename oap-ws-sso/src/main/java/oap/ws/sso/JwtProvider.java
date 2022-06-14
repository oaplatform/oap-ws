package oap.ws.sso;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JwtProvider {

    private final String secret;

    public JwtProvider( String secret ) {
        this.secret = secret;
    }

    public String generateToken( User user ) throws JWTCreationException {
        Algorithm algorithm = Algorithm.HMAC256( secret );
        return JWT.create()
            .withClaim( "user", user.getEmail() )
            .withClaim( "role", user.getRole() )
            .withClaim( "organisation", user.getOrganisationId() )
            .withIssuer( "auth0" )
            .sign( algorithm );
    }

    public DecodedJWT verifyToken( String token ) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256( secret );
        JWTVerifier verifier = JWT.require( algorithm )
            .withIssuer( "auth0" )
            .build();
        return verifier.verify( token );
    }
}
