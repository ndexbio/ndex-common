/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.dao;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.model.exceptions.NdexException;

import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRequestDAO  {
	
	/*
	// instantiate the correct type of dao for testing
	private static final RequestDAO dao = DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
			.get().getRequestDAO();
	
	 @Test
    public void createRequest() throws IllegalArgumentException, NdexException
    {
        Assert.assertTrue(createNewRequest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRequestInvalid() throws IllegalArgumentException, NdexException
    {
        dao.createRequest(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRequestInvalidRequestType() throws IllegalArgumentException, NdexException
    {
        final Request newRequest = new Request();
        newRequest.setFrom(this.testUserName);
        newRequest.setFromId(IdConverter.toJid(getRid(this.testUserName)));
        newRequest.setMessage(this.testRquestMessage);
        newRequest.setRequestType("Bogus Request Type");
        newRequest.setTo(this.testNetworkName);
        newRequest.setToId(IdConverter.toJid(getRid(this.testNetworkName)));
        
        dao.createRequest(newRequest);
    }

    @Test
    public void deleteRequest() throws IllegalArgumentException, NdexException
    {
        Assert.assertTrue(createNewRequest());

        final ORID testRequestRid = getRid(this.testRquestMessage);
        Assert.assertTrue(deleteTargetRequest(IdConverter.toJid(testRequestRid)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteRequestInvalid() throws IllegalArgumentException, NdexException
    {
        dao.deleteRequest("");
    }

    @Test
    public void getRequest()
    {
        try
        {
            final ORID testRequestRid = getRid(this.testRquestMessage);
            final Request testRequest = dao.getRequest(IdConverter.toJid(testRequestRid));
            Assert.assertNotNull(testRequest);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRequestInvalid() throws IllegalArgumentException, NdexException
    {
        dao.getRequest("");
    }

    @Test
    public void updateRequest()
    {
        try
        {
            Assert.assertTrue(createNewRequest());
            
            final ORID testRequestRid = getRid(this.testRquestMessage);

            final Request testRequest = dao.getRequest(IdConverter.toJid(testRequestRid));
            testRequest.setResponse("DECLINED");
            testRequest.setResponseMessage("Because this is a test.");
            testRequest.setResponder(testRequest.getToId());

            dao.updateRequest(testRequest);
            Assert.assertEquals(dao.getRequest(testRequest.getId()).getResponse(), testRequest.getResponse());
            
            Assert.assertTrue(deleteTargetRequest(testRequest.getId()));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void updateRequestInvalid() throws IllegalArgumentException, NdexException
    {
        dao.updateRequest(null);
    }
    
	
	 private boolean createNewRequest() throws IllegalArgumentException, NdexException
	    {
	        final Request newRequest = new Request();
	        newRequest.setFrom(this.testUserName);
	        newRequest.setFromId(IdConverter.toJid(getRid("biologist1")));
	        newRequest.setMessage(this.testRquestMessage);
	        newRequest.setRequestType("Network Access");
	        newRequest.setTo(this.testNetworkName);
	        newRequest.setToId(IdConverter.toJid(getRid(this.testNetworkName)));
	        
	        try
	        {
	            dao.createRequest(newRequest);
	            return true;
	        }
	        catch (DuplicateObjectException doe)
	        {
	            return true;
	        }
	        catch (Exception e)
	        {
	            Assert.fail(e.getMessage());
	            e.printStackTrace();
	        }
	        
	        return false;
	    }
	    
	    private boolean deleteTargetRequest(String requestId)
	    {
	        try
	        {
	            dao.deleteRequest(requestId);
	            Assert.assertNull(dao.getRequest(requestId));
	            
	            return true;
	        }
	        catch (Exception e)
	        {
	            Assert.fail(e.getMessage());
	            e.printStackTrace();
	        }
	        
	        return false;
	    }
*/
}
