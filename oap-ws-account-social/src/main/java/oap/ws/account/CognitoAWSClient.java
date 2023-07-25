package oap.ws.account;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityResponse;
import software.amazon.awssdk.services.cognitoidentity.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class CognitoAWSClient {

    // Replace these values with your actual Cognito configuration
    private static final String USER_POOL_ID = "eu-north-1_oDMuWz82Y";
    private static final String CLIENT_ID = "53s0h0rlinjdq51ugvgfe1dp1f";
    private static final String IDENTITY_POOL_ID = "eu-north-1:37ca53f2-bfd6-4bf5-8d16-83034837541a";
    private static final String CALLBACK_URL = "https://d7da-95-67-108-63.ngrok-free.app";
    private static final String USERNAME = "yowoxef169@ridteam.com";
    private static final String PASSWORD = "Yowoxef169@ridteam.com";
    private static final String PASSWORD_NEW = "Yowoxef169@ridteam.com!";

    public static void main( String[] args ) throws CognitoIdentityProviderException {

        CognitoIdentityProviderClient identityProviderClient = CognitoIdentityProviderClient.builder()
            .region( Region.EU_NORTH_1 )
            .credentialsProvider( ProfileCredentialsProvider.create() )
            .build();

        InitiateAuthResponse authResult = initiateAuthentication( identityProviderClient, CLIENT_ID, USERNAME, PASSWORD_NEW, USER_POOL_ID );
        if( authResult.challengeName() == null ) {
            // No challenge, user is authenticated directly.
            String accessToken = authResult.authenticationResult().accessToken();
            String idToken = authResult.authenticationResult().idToken();
            // Do something with the access token and id token as needed.
            System.out.println( "accessToken: " + accessToken );
            getUserAttributesFromAccessToken( identityProviderClient, accessToken );
            System.out.println( idToken );
        } else if( ChallengeNameType.NEW_PASSWORD_REQUIRED.equals( authResult.challengeName() ) ) {
            // Handle the case where the user needs to set a new password.
            // You can redirect the user to a password change page.
            String session = authResult.session();
            String newPasswordRequiredURL = CALLBACK_URL + "?session=" + session;
            System.out.println( newPasswordRequiredURL );
            respondToNewPasswordChallenge( identityProviderClient, session, PASSWORD_NEW );
        } else {
            throw CognitoIdentityProviderException.builder().message( "ANOTHER CHALLENGE " + authResult.challengeName() ).build();
        }
    }


    private static void changeUserPassword( String username, String oldPassword, String newPassword ) {

        CognitoIdentityProviderClient cognitoIdentityProvider = CognitoIdentityProviderClient.builder()
            .credentialsProvider( ProfileCredentialsProvider.create() )
            .region( Region.EU_NORTH_1 )
            .build();

        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
            .previousPassword( oldPassword )
            .proposedPassword( newPassword )
            .accessToken( getAccessToken( username, oldPassword ) ).build();

        try {
            final ChangePasswordResponse changePasswordResponse = cognitoIdentityProvider.changePassword( changePasswordRequest );
            System.out.println( "Password changed successfully!" );
            // You can handle the result as needed
        } catch( NotAuthorizedException e ) {
            System.out.println( "Password change failed. Not authorized." );
            // Handle the case where the user's current password is incorrect.
        } catch( UserNotFoundException e ) {
            System.out.println( "User not found." );
            // Handle the case where the user is not found in the Cognito User Pool.
        } catch( Exception e ) {
            System.out.println( "An error occurred: " + e.getMessage() );
            // Handle other exceptions or errors as needed.
        }
    }

    private static String getAccessToken( String username, String password ) {
        // Implement your method to obtain the access token for the user.
        // You can use Cognito authentication APIs to authenticate the user and get the access token.
        // For simplicity, in this example, we are assuming you already have an access token.
        // Note: It's essential to authenticate the user before changing the password to ensure security.
        return "YOUR_ACCESS_TOKEN";
    }

    public static InitiateAuthResponse initiateAuthentication( CognitoIdentityProviderClient identityProviderClient, String clientId, String userName, String password, String userPoolId ) {
        try {
            Map<String, String> authParameters = new HashMap<>();
            authParameters.put( "USERNAME", userName );
            authParameters.put( "PASSWORD", password );

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId( clientId )
                .authParameters( authParameters )
                .authFlow( AuthFlowType.USER_PASSWORD_AUTH )
                .build();

            InitiateAuthResponse response = identityProviderClient.initiateAuth( authRequest );
            System.out.println( "Result Challenge is : " + response.challengeName() );
            return response;

        } catch( CognitoIdentityProviderException e ) {
            System.err.println( e.awsErrorDetails().errorMessage() );
            System.exit( 1 );
        }

        return null;
    }

    public static void getCredsForIdentity( CognitoIdentityClient cognitoClient ) {
        try {
            GetCredentialsForIdentityRequest getCredentialsForIdentityRequest = GetCredentialsForIdentityRequest.builder()
                .identityId( IDENTITY_POOL_ID )
                .build();

            GetCredentialsForIdentityResponse response = cognitoClient.getCredentialsForIdentity( getCredentialsForIdentityRequest );
            System.out.println( "Identity ID " + response.identityId() + ", Access key ID " + response.credentials().accessKeyId() );

        } catch( CognitoIdentityProviderException e ) {
            System.err.println( e.awsErrorDetails().errorMessage() );
            System.exit( 1 );
        }
    }

    private static void respondToNewPasswordChallenge( CognitoIdentityProviderClient cognitoIdentityProvider, String session, String newPassword ) {
        RespondToAuthChallengeRequest challengeRequest = RespondToAuthChallengeRequest.builder()
            .clientId( CLIENT_ID )
            .challengeName( ChallengeNameType.NEW_PASSWORD_REQUIRED )
            .session( session )
            .challengeResponses( Map.of( "NEW_PASSWORD", PASSWORD_NEW, "USERNAME", USERNAME ) ).build();

        final RespondToAuthChallengeResponse respondToAuthChallengeResponse = cognitoIdentityProvider.respondToAuthChallenge( challengeRequest );

        System.out.println( respondToAuthChallengeResponse.authenticationResult() );
    }

    private static void getUserAttributesFromAccessToken( CognitoIdentityProviderClient cognitoIdentityProvider, String accessToken ) {
        GetUserRequest getUserRequest = GetUserRequest.builder().accessToken( accessToken ).build();

        final GetUserResponse user = cognitoIdentityProvider.getUser( getUserRequest );
        for( AttributeType attribute : user.userAttributes() ) {
            System.out.println( attribute.name() + ": " + attribute.value() );
        }
    }
}
