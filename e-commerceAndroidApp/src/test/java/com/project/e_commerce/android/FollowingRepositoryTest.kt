package com.project.e_commerce.android

import com.project.e_commerce.android.domain.model.FollowRelationship
import com.project.e_commerce.android.domain.model.FollowingStatus
import org.junit.Test
import org.junit.Assert.*

class FollowingRepositoryTest {
    
    @Test
    fun testFollowRelationshipCreation() {
        val followRel = FollowRelationship(
            id = "user1_user2",
            followerId = "user1",
            followedId = "user2",
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals("user1_user2", followRel.id)
        assertEquals("user1", followRel.followerId)
        assertEquals("user2", followRel.followedId)
        assertTrue(followRel.createdAt > 0)
    }
    
    @Test
    fun testFollowingStatus() {
        val status = FollowingStatus(
            isFollowing = true,
            isFollowedBy = false,
            isMutual = false
        )
        
        assertTrue(status.isFollowing)
        assertFalse(status.isFollowedBy)
        assertFalse(status.isMutual)
    }
    
    @Test
    fun testMutualFollowing() {
        val status = FollowingStatus(
            isFollowing = true,
            isFollowedBy = true,
            isMutual = true
        )
        
        assertTrue(status.isFollowing)
        assertTrue(status.isFollowedBy)
        assertTrue(status.isMutual)
    }
}
