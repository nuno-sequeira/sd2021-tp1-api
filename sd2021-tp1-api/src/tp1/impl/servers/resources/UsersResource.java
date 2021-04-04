package tp1.impl.servers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

@Singleton
public class UsersResource implements RestUsers {

    private final Map<String,User> users = new HashMap<String, User>();

    private static Logger Log = Logger.getLogger(UsersResource.class.getName());

    public UsersResource() {
    }

    @Override
    public String createUser(User user) {
        Log.info("createUser : " + user);

        // Check if user is valid, if not return HTTP CONFLICT (409)
        if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null ||
                user.getEmail() == null) {
            Log.info("User object invalid.");
            throw new WebApplicationException( Status.CONFLICT );
        }

        synchronized (this) {

            // Check if userId does not exist exists, if not return HTTP CONFLICT (409)
            if( users.containsKey(user.getUserId())) {
                Log.info("User already exists.");
                throw new WebApplicationException( Status.CONFLICT );
            }

            //Add the user to the map of users
            users.put(user.getUserId(), user);

        }

        return user.getUserId();
    }


    @Override
    public User getUser(String userId, String password) {
        Log.info("getUser : user = " + userId + "; pwd = " + password);

        // Check if user is valid, if not return HTTP CONFLICT (409)
        if(userId == null || password == null) {
            Log.info("UserId or passwrod null.");
            throw new WebApplicationException( Status.CONFLICT );
        }

        User user = null;

        synchronized(this) {

            user = users.get(userId);

            // Check if user exists
            if( user == null ) {
                Log.info("User does not exist.");
                throw new WebApplicationException( Status.NOT_FOUND );
            }

            //Check if the password is correct
            if(!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                throw new WebApplicationException( Status.FORBIDDEN );
            }
        }

        return user;
    }


    @Override
    public User updateUser(String userId, String password, User user) {
        Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);

        // Check if user is valid, if not return HTTP CONFLICT (409)
        if(userId == null || password == null) {
            Log.info("UserId or passwrod or user null.");
            throw new WebApplicationException( Status.CONFLICT );
        }

        User userStored = null;

        synchronized(this) {

            //Gets already stored user
            userStored = users.get(userId);


            //Checks if user exists and given password is the same that is stored. If not, it returns HTTP FORBIDDEN(403)
            if(userStored == null || !userStored.getPassword().equalsIgnoreCase(password)){
                Log.info ("Password is invalid.");
                throw new WebApplicationException( Status.FORBIDDEN );
            }
            //If received email is not null, we update it
            if(user.getEmail() != null) userStored.setEmail(user.getEmail());
            //If received fullName is not null, we update it
            if(user.getFullName() != null) userStored.setFullName(user.getFullName());
            //If received email is not null, we update it
            if(user.getPassword() != null && !user.getPassword().isEmpty()) userStored.setPassword(user.getPassword());

            //Save updated user
            users.put(userId, userStored);

        }

        return userStored;
    }


    @Override
    public User deleteUser(String userId, String password) {
        Log.info("deleteUser : user = " + userId + "; pwd = " + password);

        if(userId == null || password == null) {
            Log.info("UserId or passwrod null.");
            throw new WebApplicationException( Status.CONFLICT );
        }

        User user = null;

        synchronized(this) {

            user = users.get(userId);

            // Check if user exists
            if(user == null) {
                Log.info("User does not exist.");
                throw new WebApplicationException( Status.NOT_FOUND );
            }

            //Check if the password is correct
            if(!user.getPassword().equals(password)) {
                Log.info("Password is incorrect.");
                throw new WebApplicationException( Status.FORBIDDEN );
            }

            return users.remove(userId);

        }
    }


    @Override
    public List<User> searchUsers(String pattern) {
        Log.info("searchUsers : pattern = " + pattern);

        if(pattern == null) {
            pattern = "";
        }

        List<User> matchUsers = null;

        synchronized(this) {

            //Gets set of existing users
            Set<String> namesSet = users.keySet();
            //Instantiates match users
            matchUsers = new LinkedList<>();

            //For each existing user name
            for(String currentName : namesSet){
                //Gets respective user
                User currentUser = users.get(currentName);
                //If its full name contains pattern we add it to the match users, removing the password from it
                if(currentUser.getFullName().contains(pattern)) {
                    currentUser.setPassword("");
                    matchUsers.add(currentUser);
                }
            }

        }

        //Returns match users
        return matchUsers;
    }

}