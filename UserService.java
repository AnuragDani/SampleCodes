package com.pchinta.punchclock.web.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pchinta.punchclock.web.JPAException;
import com.pchinta.punchclock.web.JpaService;
import com.pchinta.punchclock.web.gcm.GCMSender;
import com.pchinta.punchclock.web.model.User;
import com.pchinta.punchclock.web.model.UserStatus;

@Path("/user")
public class UserService {

	private JpaService jpaService = JpaService.getJPAService();
	private static Logger logger = LoggerFactory.getLogger(UserService.class);

	ObjectMapper objectMapper = new ObjectMapper();

	@Path("/save")
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public boolean save(@QueryParam("username") String username,
			@QueryParam("deviceId") String deviceId,
			@QueryParam("regId") String regId) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		// validate user
		User user = jpaService.findById(User.class, deviceId);
		if (user != null && deviceId != null) {
			user.setName(username);
			user.setRegId(regId);
			user.setStatus(true);
			user.setNear(false);
			jpaService.persist(user);
			String chatUserId = ChatService.updateUser(user);
			user.setChatId(chatUserId);
			return true;
		} else if (deviceId != null) {
			user = new User();
			user.setRegId(regId);
			user.setName(username);
			user.setDeviceId(deviceId);
			user.setStatus(true);
			user.setNear(false);
			String chatUserId = ChatService.createUser(user);
			user.setChatId(chatUserId);
			jpaService.persist(user);
			return true;
		}
		return false;
	}

	@Path("/updateNearBeacon")
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public boolean updateNearBeacon(@QueryParam("username") String username,
			@QueryParam("isNear") boolean isNear) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		// validate user
		User user = getUserByName(username);
		if (user != null) {
			user.setNear(isNear);
			user.setStatus(true);
			jpaService.persist(user);
			List<User> followedBy = user.getFollowedBy();
			if (followedBy != null && !followedBy.isEmpty()) {
				List<String> targets = new ArrayList<String>();
				for (int i = 0; i < followedBy.size(); i++) {
					User u = followedBy.get(i);
					targets.add(u.getRegId());
				}

				String message = user.getName() + " has "
						+ (isNear ? "entered" : "exited")
						+ " the beacon region";
				GCMSender.sendNotification(targets, message);
			}
			return true;
		}
		return false;
	}

	@Path("/updateInsideGeofence")
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public boolean updateInsideGeofence(
			@QueryParam("username") String username,
			@QueryParam("isInside") boolean isInside) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		// validate user
		User user = getUserByName(username);
		if (user != null) {
			user.setInside(isInside);
			user.setStatus(true);
			jpaService.persist(user);
			List<User> followedBy = user.getFollowedBy();
			if (followedBy != null && !followedBy.isEmpty()) {
				List<String> targets = new ArrayList<String>();
				for (int i = 0; i < followedBy.size(); i++) {
					User u = followedBy.get(i);
					targets.add(u.getRegId());
				}

				String message = user.getName() + " has "
						+ (isInside ? "entered" : "exited") + " the geofence";
				GCMSender.sendNotification(targets, message);
			}
			return true;
		}
		return false;
	}

	@Path("/follow")
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public boolean follow(@QueryParam("currentuser") String username,
			@QueryParam("tofollowuser") String toFollow) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		// validate user
		User user = getUserByName(username);
		User followUser = getUserByName(toFollow);

		if (user != null && followUser != null && !user.equals(followUser)) {
			if (!user.getFollowing().contains(followUser)) {
				user.getFollowing().add(followUser);
				jpaService.persist(user);
			}
			return true;
		} else {
			return false;
		}
	}

	@Path("/unfollow")
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public boolean unfollow(@QueryParam("currentuser") String username,
			@QueryParam("tofollowuser") String toFollow) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		// validate user
		User user = getUserByName(username);
		User followUser = getUserByName(toFollow);
		if (user != null && followUser != null && !user.equals(followUser)) {
			if (user.getFollowing().contains(followUser)) {
				user.getFollowing().remove(followUser);
				jpaService.persist(user);
			}
			return true;
		} else {
			return false;
		}
	}

	@Path("/fetchAll")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String fetchAll() throws JPAException, JsonGenerationException,
			JsonMappingException, IOException {
		List<User> allUsers = jpaService.findAll(User.class, "findAllUsers",
				null);

		return objectMapper.writeValueAsString(allUsers);
	}

	@Path("/findAllFollowedByUser")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String findAllFollowedByUser(@QueryParam("username") String username)
			throws JPAException, JsonGenerationException, JsonMappingException,
			IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", username);
		List<User> allUsers = jpaService.findAll(User.class,
				"findAllFollowedByUser", params);

		return objectMapper.writeValueAsString(allUsers.get(0).getFollowing());
	}

	@Path("/findAllFollowingThisUser")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String findAllFollowingThisUser(
			@QueryParam("username") String username) throws JPAException,
			JsonGenerationException, JsonMappingException, IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", username);
		List<User> allUsers = jpaService.findAll(User.class,
				"findAllFollowingThisUser", params);

		return objectMapper.writeValueAsString(allUsers.get(0).getFollowedBy());
	}

	@Path("/findAllUsersWithStatus")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String findAllUsersWithStatus(@QueryParam("username") String username)
			throws JPAException, JsonGenerationException, JsonMappingException,
			IOException {
		User user = getUserByName(username);
		List<User> allUsers = jpaService.findAll(User.class, "findAllUsers",
				null);
		List<User> followers = user.getFollowedBy();
		List<User> following = user.getFollowing();
		List<UserStatus> usersWithStatus = new ArrayList<UserStatus>();
		for (User u : allUsers) {
			if (!u.equals(user)) {
				UserStatus us = new UserStatus();
				us.setName(u.getName());
				us.setIn(u.isNear());
				us.setWatchingYou(followers.contains(u));
				us.setWatchedByYou(following.contains(u));
				usersWithStatus.add(us);
			}
		}
		return objectMapper.writeValueAsString(usersWithStatus);
	}

	@Path("/findByDevice")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String findByDevice(@QueryParam("deviceId") String id)
			throws JPAException, JsonGenerationException, JsonMappingException,
			IOException {
		logger.debug("Fetching user with ID : " + id);
		User user = jpaService.findById(User.class, id);
		if (user != null) {
			logger.debug("User name is " + user.getName());
			return user.getName();
		} else
			logger.debug("user not found with ID " + id);
		return null;
	}

	@Path("/findByName")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String findByName(@QueryParam("username") String username)
			throws JPAException, JsonGenerationException, JsonMappingException,
			IOException {
		logger.debug("Fetching user with ID : " + username);
		User user = getUserByName(username);

		return objectMapper.writeValueAsString(user);
	}

	private User getUserByName(String username) throws JPAException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("username", username);
		List<User> users = jpaService.findAll(User.class, "findUserByName",
				params);
		if (users.isEmpty())
			return null;
		User user = users.get(0);
		return user;
	}

}
