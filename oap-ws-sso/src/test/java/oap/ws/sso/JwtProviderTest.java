package oap.ws.sso;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtProviderTest extends IntegratedTest {

    private final JwtProvider jwtProvider = new JwtProvider( "secret" );
    private final TestUser testUser = new TestUser( "john.doe@gmail.com", "qwerty", Roles.ADMIN, "companyId" );
    private final String testToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJpc3MiOiJhdXRoMCIsIm9yZ2FuaXNhdGlvbiI6ImNvbXBhbnlJZCIsInVzZXIiOiJqb2huLmRvZUBnbWFpbC5jb20ifQ.RCR5nqmGGDLmfSQzOyMTQlI6tU1Pl1QsmzqnzHhDhOM";


    @Test
    public void generateTokenTest() {
        final String generatedJwt = jwtProvider.generateToken( testUser );
        assertThat( generatedJwt ).isEqualTo( testToken );
    }

    @Test
    public void verifyTokenTest() {
        final DecodedJWT decodedJWT = jwtProvider.verifyToken( testToken );
        final Map<String, Claim> claims = decodedJWT.getClaims();
        assertThat( claims.get( "user" ).asString() ).isEqualTo( testUser.getEmail() );
        assertThat( claims.get( "role" ).asString() ).isEqualTo( testUser.getRole() );
        assertThat( claims.get( "organisation" ).asString() ).isEqualTo( testUser.getOrganisationId() );
    }
}
