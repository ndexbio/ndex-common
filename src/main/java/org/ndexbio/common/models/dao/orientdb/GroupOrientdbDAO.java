/**
 * 
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.List;
import java.util.Date;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.helpers.Validation;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.common.models.data.IGroup;
import org.ndexbio.common.models.data.IGroupMembership;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.model.object.SearchParameters;
import org.ndexbio.common.models.object.privilege.Group;
import org.ndexbio.common.models.object.privilege.Membership;
import org.ndexbio.common.models.object.privilege.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientElement;

/**
 * @author fcriscuo
 * 
 */
public class GroupOrientdbDAO extends OrientdbDAO {

	private final Logger logger = LoggerFactory
			.getLogger(GroupOrientdbDAO.class);
	
	 private GroupOrientdbDAO() {super();}
	 
	 static GroupOrientdbDAO createInstance() { return new GroupOrientdbDAO() ; }

	
	
	public Group createGroup(Group newGroup, String userId)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {

		Preconditions.checkArgument(null != newGroup, "A group is required");
		Preconditions.checkState(this.isValidGroupName(newGroup), "Group "
				+ newGroup.getName() + " already exists");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");
		try {

	
			setupDatabase();
			final IUser user = this.findIuserById(userId);
		
			final IUser groupOwner = _orientDbGraph.getVertex(
					userId,
					IUser.class);
			final IGroup group = _orientDbGraph.addVertex("class:group",
					IGroup.class);
			group.setDescription(newGroup.getDescription());
			group.setName(newGroup.getName());
			group.setOrganizationName(newGroup.getOrganizationName());
			group.setWebsite(newGroup.getWebsite());
			group.setCreatedDate(new Date());
			addGroupMembers(newGroup, groupOwner, group);
			newGroup.setId(IdConverter.toJid((ORID) group.asVertex().getId()));
			return newGroup;
		} catch (Exception e) {

			this.logger.error("Failed to create group: " + newGroup.getName()
					+ ".", e);
			throw new NdexException("Failed to create your group.");
		} finally {
			teardownDatabase();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ndexbio.common.models.dao.GroupDAO#deleteGroup(java.lang.String)
	 */
	
	public void deleteGroup(String groupId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId),
				"No group ID was specified.");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required.");

		final ORID groupRid = IdConverter.toRid(groupId);

		try {
			setupDatabase();

			final IGroup groupToDelete = _orientDbGraph.getVertex(groupRid,
					IGroup.class);
			// can this group be deleted
			validateGroupDeletionAuthorization(groupId, groupRid,
					groupToDelete, userId);

			for (IGroupMembership groupMembership : groupToDelete.getMembers()) {
				groupMembership.setMember(null);
				groupMembership.setGroup(null);
				_orientDbGraph.removeVertex(groupMembership.asVertex());

			}

			final List<ODocument> groupChildren = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT @RID FROM (TRAVERSE * FROM "
									+ groupRid
									+ " WHILE @Class <> 'user' and @Class <> 'group')"));
			for (ODocument groupChild : groupChildren) {
				final OrientElement element = _orientDbGraph.getBaseGraph()
						.getElement(groupChild.field("rid", OType.LINK));
				if (element != null)
					element.remove();
			}

			_orientDbGraph.removeVertex(groupToDelete.asVertex());
			_orientDbGraph.getBaseGraph().commit();
		} catch (SecurityException | NdexException ne) {
			this.logger.error(ne.getMessage());
			throw ne;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1) {
				this.logger
						.error("Group to be deleted, not found in database.");
				throw new ObjectNotFoundException("Group", groupId);
			}

			this.logger.error("Failed to delete group: " + groupId + ".", e);

			throw new NdexException("Failed to delete the group.");
		} finally {
			teardownDatabase();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ndexbio.common.models.dao.GroupDAO#findGroups(org.ndexbio.common.
	 * models.object.SearchParameters, java.lang.String)
	 */
	
	public List<Group> findGroups(SearchParameters searchParameters,
			String searchOperator) throws IllegalArgumentException,
			NdexException {

		Preconditions.checkArgument(null != searchParameters,
				"A SearchParameters object is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(searchParameters.getSearchString()),
				"A search string is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(searchOperator),
				"A search operator is required");
		final List<Group> foundGroups = Lists.newArrayList();

		String query = determinGroupQuery(searchParameters, searchOperator);

		if (!Strings.isNullOrEmpty(query)) {
			try {
				setupDatabase();

				final List<ODocument> groups = _ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(query));
				for (final ODocument group : groups)
					foundGroups.add(new Group(_orientDbGraph.getVertex(group,
							IGroup.class)));

			} catch (Exception e) {
				this.logger.error("Failed to search groups: "
						+ searchParameters.getSearchString(), e);

				throw new NdexException("Failed to search groups.");
			} finally {
				teardownDatabase();
			}
		} else {
			logger.error("A group query could not be determined.");
		}
		return foundGroups;
	}

	private String determinGroupQuery(SearchParameters searchParameters,
			String searchOperator) {
		searchParameters.setSearchString(searchParameters.getSearchString()
				.toLowerCase().trim());
		String operator = searchOperator.toLowerCase();
		final int startIndex = searchParameters.getSkip()
				* searchParameters.getTop();
		String query = "";
		switch (operator) {
		case CommonDAOValues.SEARCH_MATCH_EXACT:
			query = "SELECT FROM Group\n" + "WHERE name.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "'\n"
					+ "  OR description.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "'\n"
					+ "  OR organizationName.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
			break;
		case CommonDAOValues.SEARCH_MATCH_STARTS_WITH:
			query = "SELECT FROM Group\n" + "WHERE name.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR description.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR organizationName.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
			break;
		case CommonDAOValues.SEARCH_MATCH_CONTAINS:
			query = "SELECT FROM Group\n" + "WHERE name.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR description.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR organizationName.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
			break;
		default:

		} // end of switch clause
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ndexbio.common.models.dao.GroupDAO#getGroup(java.lang.String)
	 */
	
	public Group getGroup(String groupId) throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId),
				"A group id is required");

		try {
			setupDatabase();

			final IGroup group = _orientDbGraph.getVertex(
					IdConverter.toRid(groupId), IGroup.class);
			if (group != null)
				return new Group(group, true);
		} catch (IllegalArgumentException iae) {
			// The group ID is actually a group name
			final List<ODocument> matchingGroups = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT FROM Group WHERE name = '" + groupId + "'"));
			if (!matchingGroups.isEmpty())
				return new Group(_orientDbGraph.getVertex(
						matchingGroups.get(0), IGroup.class), true);
		} catch (Exception e) {
			this.logger.error("Failed to retrieve group: " + groupId + ".", e);
			throw new NdexException("Failed to retrieve the group.");
		} finally {
			teardownDatabase();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ndexbio.common.models.dao.GroupDAO#removeMember(java.lang.String,
	 * java.lang.String)
	 */
	
	public void removeMember(String groupId, String memberId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId),
				"A group id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(memberId),
				"A group member id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id  is required");

		try {
			setupDatabase();

			final ORID groupRid = IdConverter.toRid(groupId);
			final IGroup group = _orientDbGraph.getVertex(groupRid,
					IGroup.class);
			if (group == null)
				throw new ObjectNotFoundException("Group", groupId);
			else if (!hasPermission(new Group(group), Permissions.ADMIN,
					memberId))
				throw new SecurityException("Access denied.");

			final IUser user = _orientDbGraph.getVertex(
					IdConverter.toRid(memberId), IUser.class);
			if (user == null)
				throw new ObjectNotFoundException("User", memberId);

			for (IGroupMembership groupMember : group.getMembers()) {
				String groupMemberId = IdConverter.toJid((ORID) groupMember
						.getMember().asVertex().getId());
				if (groupMemberId.equals(memberId)) {
					if (countAdminMembers(groupRid) < 2)
						throw new SecurityException(
								"Cannot remove the only ADMIN member.");

					group.removeMember(groupMember);
					user.removeGroup(groupMember);
					_orientDbGraph.getBaseGraph().commit();
					return;
				}
			}
		} catch (ObjectNotFoundException | SecurityException ne) {
			throw ne;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1) {
				this.logger.error("Group id " + groupId + " not found");
				throw new ObjectNotFoundException("Group", groupId);
			}
			this.logger.error("Failed to remove member.", e);

			throw new NdexException("Failed to remove member.");
		} finally {
			teardownDatabase();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ndexbio.common.models.dao.GroupDAO#updateGroup(org.ndexbio.common
	 * .models.object.Group)
	 */
	
	public void updateGroup(Group updatedGroup, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException {
		Preconditions.checkArgument(null != updatedGroup,
				"A Group is required.");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");

		try {
			setupDatabase();

			final IGroup groupToUpdate = _orientDbGraph.getVertex(
					IdConverter.toRid(updatedGroup.getId()), IGroup.class);
			if (groupToUpdate == null)
				throw new ObjectNotFoundException("Group", updatedGroup.getId());
			else if (!hasPermission(updatedGroup, Permissions.WRITE, userId))
				throw new SecurityException("Access denied.");

			if (updatedGroup.getDescription() != null
					&& !updatedGroup.getDescription().equals(
							groupToUpdate.getDescription()))
				groupToUpdate.setDescription(updatedGroup.getDescription());

			if (updatedGroup.getName() != null
					&& !updatedGroup.getName().equals(groupToUpdate.getName()))
				groupToUpdate.setName(updatedGroup.getName());

			if (updatedGroup.getOrganizationName() != null
					&& !updatedGroup.getOrganizationName().equals(
							groupToUpdate.getOrganizationName()))
				groupToUpdate.setOrganizationName(updatedGroup
						.getOrganizationName());

			if (updatedGroup.getWebsite() != null
					&& !updatedGroup.getWebsite().equals(
							groupToUpdate.getWebsite()))
				groupToUpdate.setWebsite(updatedGroup.getWebsite());

		} catch (SecurityException | ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1)
				throw new ObjectNotFoundException("Group", updatedGroup.getId());

			this.logger.error(
					"Failed to update group: " + updatedGroup.getName() + ".",
					e);

			throw new NdexException("Failed to update the group.");
		} finally {
			teardownDatabase();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ndexbio.common.models.dao.GroupDAO#updateMember(java.lang.String,
	 * org.ndexbio.common.models.object.Membership)
	 */
	
	public void updateMember(String groupId, Membership groupMember,
			String userId) throws IllegalArgumentException,
			ObjectNotFoundException, SecurityException, NdexException {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(groupId),
				"A group id is required");
		Preconditions.checkArgument(null != groupMember,
				"A group member is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(groupMember.getResourceId()),
				"A resource id is required for the group member");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");

		try {
			setupDatabase();

			final ORID groupRid = IdConverter.toRid(groupId);
			final IGroup group = _orientDbGraph.getVertex(groupRid,
					IGroup.class);

			if (group == null)
				throw new ObjectNotFoundException("Group", groupId);
			else if (!hasPermission(new Group(group), Permissions.ADMIN, userId))
				throw new SecurityException("Access denied.");

			final IUser user = _orientDbGraph
					.getVertex(IdConverter.toRid(groupMember.getResourceId()),
							IUser.class);
			if (user == null)
				throw new ObjectNotFoundException("User",
						groupMember.getResourceId());

			for (IGroupMembership groupMembership : group.getMembers()) {
				final String memberId = IdConverter
						.toJid((ORID) groupMembership.getMember().asVertex()
								.getId());
				if (memberId.equals(groupMember.getResourceId())) {
					if (countAdminMembers(groupRid) < 2) {
						throw new SecurityException(
								"Cannot change the permissions on the only ADMIN member.");
					}
					groupMembership
							.setPermissions(groupMember.getPermissions());

					return;
				}
			}
		} catch (ObjectNotFoundException | SecurityException ne) {
			throw ne;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1) {
				throw new ObjectNotFoundException("Either the Group " + groupId
						+ " or User " + groupMember.getResourceId()
						+ " was not found.");
			} else {
				this.logger.error(
						"Failed to update member: "
								+ groupMember.getResourceName() + ".", e);

				throw new NdexException("Failed to update member: "
						+ groupMember.getResourceName() + ".");
			}
		} finally {
			teardownDatabase();
		}

	}

	private void addGroupMembers(final Group newGroup, final IUser groupOwner,
			final IGroup group) {
		if (newGroup.getMembers() == null || newGroup.getMembers().size() == 0) {
			final IGroupMembership membership = _orientDbGraph.addVertex(
					"class:groupMembership", IGroupMembership.class);
			membership.setPermissions(Permissions.ADMIN);
			membership.setMember(groupOwner);
			membership.setGroup(group);

		} else {
			for (Membership member : newGroup.getMembers()) {
				final IUser groupMember = _orientDbGraph.getVertex(
						IdConverter.toRid(member.getResourceId()), IUser.class);

				final IGroupMembership membership = _orientDbGraph.addVertex(
						"class:groupMembership", IGroupMembership.class);
				membership.setPermissions(member.getPermissions());
				membership.setMember(groupMember);
				membership.setGroup(group);

				groupMember.addGroup(membership);
				group.addMember(membership);
			}
		}
	}

	private void validateGroupDeletionAuthorization(final String groupId,
			final ORID groupRid, final IGroup groupToDelete, final String userId)
			throws ObjectNotFoundException, NdexException {
		if (groupToDelete == null)
			throw new ObjectNotFoundException("Group", groupId);
		else if (!hasPermission(new Group(groupToDelete), Permissions.ADMIN,
				userId))
			throw new SecurityException(
					"Insufficient privileges to delete the group.");

		final List<ODocument> adminCount = _ndexDatabase
				.query(new OSQLSynchQuery<Integer>(
						"SELECT COUNT(@RID) FROM GroupMembership WHERE in_groupMembers = "
								+ groupRid + " AND permissions = 'ADMIN'"));
		if (adminCount == null || adminCount.isEmpty())
			throw new NdexException("Unable to count ADMIN members.");
		else if ((long) adminCount.get(0).field("COUNT") > 1)
			throw new NdexException(
					"Cannot delete a group that contains other ADMIN members.");

		final List<ODocument> adminNetworks = _ndexDatabase
				.query(new OSQLSynchQuery<Integer>(
						"SELECT COUNT(@RID) FROM Membership WHERE in_userNetworks = "
								+ groupRid + " AND permissions = 'ADMIN'"));
		if (adminCount == null || adminCount.isEmpty())
			throw new NdexException("Unable to query group/network membership.");
		else if ((long) adminNetworks.get(0).field("COUNT") > 1)
			throw new NdexException(
					"Cannot delete a group that is an ADMIN member of any network.");
		this.logger.info("OK to delete group id " + groupId);
	}

	/*
	 * private method to determine if a proposed group name is novel
	 * 
	 * @params groupName - new group name
	 * 
	 * @returns boolean - true if group name is new false id group name already
	 * exists
	 */
	private boolean isValidGroupName(Group newGroup)
			throws IllegalArgumentException, NdexException {
		try {
			Preconditions.checkNotNull(newGroup.getName(),
					"The new group requires a name");
			Preconditions.checkState(Validation.isValid(newGroup.getName(),
					Validation.REGEX_GROUP_NAME), "Invalid group name");
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

		final SearchParameters searchParameters = new SearchParameters();
		searchParameters.setSearchString(newGroup.getName());
		searchParameters.setSkip(0);
		searchParameters.setTop(1);

		List<Group> groupList = this.findGroups(searchParameters,
				CommonDAOValues.SEARCH_MATCH_EXACT);
		if (groupList.isEmpty()) {
			return true;
		}
		return false;

	}

	/**************************************************************************
	 * Count the number of administrative members in the network.
	 **************************************************************************/
	private long countAdminMembers(final ORID groupRid) throws NdexException {
		final List<ODocument> adminCount = _ndexDatabase
				.query(new OSQLSynchQuery<Integer>(
						"SELECT COUNT(@RID) FROM GroupMembership WHERE in_groupMembers = "
								+ groupRid + " AND permissions = 'ADMIN'"));
		if (adminCount == null || adminCount.isEmpty())
			throw new NdexException("Unable to count ADMIN members.");

		return (long) adminCount.get(0).field("COUNT");
	}

	/**************************************************************************
	 * Determines if the logged in user has sufficient permissions to a group.
	 * 
	 * @param targetGroup
	 *            The group to test for permissions.
	 * @return True if the member has permission, false otherwise.
	 **************************************************************************/
	private boolean hasPermission(Group targetGroup,
			Permissions requiredPermissions, String userId) {
		final IUser user = this.findIuserById(userId);
		for (IGroupMembership groupMembership : user.getGroups()) {
			if (groupMembership.getGroup().equals(targetGroup.getId())
					&& groupMembership.getPermissions().compareTo(
							requiredPermissions) > -1)
				return true;
		}

		return false;
	}

}
