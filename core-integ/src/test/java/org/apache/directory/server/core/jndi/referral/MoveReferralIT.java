/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi.referral;

import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the referral handling functionality for the Move operation 
 * within the server's core. (Move is a ModifyDN where the superior is changed)
 * 
 * All the tests are described on this page :
 * http://cwiki.apache.org/confluence/display/DIRxSRVx11/Referral+Handling+Changes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 691179 $
 */
@RunWith ( CiRunner.class )
@CleanupLevel ( Level.CLASS )
@ApplyLdifs( {
    // Root
    "dn: c=WW,ou=system\n" +
    "objectClass: country\n" +
    "objectClass: top\n" +
    "c: WW\n\n" +
    
    // Sub-root #1
    "dn: o=MNN,c=WW,ou=system\n" +
    "objectClass: organization\n" +
    "objectClass: top\n" +
    "o: MNN\n\n" +
    
    // Sub-root #2
    "dn: o=PNN,c=WW,ou=system\n" +
    "objectClass: organization\n" +
    "objectClass: top\n" +
    "o: PNN\n\n" +
    
    // Referral #1
    "dn: ou=Roles,o=MNN,c=WW,ou=system\n" +
    "objectClass: extensibleObject\n" +
    "objectClass: referral\n" +
    "objectClass: top\n" +
    "ou: Roles\n" +
    "ref: ldap://hostd/ou=Roles,dc=apache,dc=org\n\n" +
    
    // Referral #2
    "dn: ou=People,o=MNN,c=WW,ou=system\n" +
    "objectClass: extensibleObject\n" +
    "objectClass: referral\n" +
    "objectClass: top\n" +
    "ou: People\n" +
    "ref: ldap://hostb/OU=People,DC=example,DC=com\n" +
    "ref: ldap://hostc/OU=People,O=MNN,C=WW\n\n" +
    
    // Entry # 1
    "dn: cn=Alex Karasulu,o=MNN,c=WW,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Alex Karasulu\n" +
    "sn: akarasulu\n\n" +
    
    // Entry # 2
    "dn: cn=Alex,o=MNN,c=WW,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Alex\n" +
    "sn: akarasulu\n\n"
    }
)
public class MoveReferralIT
{
    /** The directory service */
    public static DirectoryService service;

    /** The Contexts we are using to inject entries with JNDI */
    LdapContext MNNCtx;
    LdapContext PNNCtx;
    LdapContext WWCtx;
    
    /** The entries we are using to do the tests */
    Attributes userEntry;
    ServerEntry serverEntry;
    
    @Before
    public void setUp() throws Exception
    {
        MNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "o=MNN,c=WW,ou=system" );
        PNNCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "o=PNN,c=WW,ou=system" );
        WWCtx = getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "c=WW,ou=system" );

        // JNDI entry
        userEntry = new BasicAttributes( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "elecharny" );
        userEntry.put( "cn", "Emmanuel Lecharny" );
        
        // Core API entry
        LdapDN dn = new LdapDN( "cn=Emmanuel Lecharny, ou=apache, ou=people, o=MNN, c=WW, ou=system" );
        serverEntry = new DefaultServerEntry( service.getSchemaManager(), dn );

        serverEntry.put( "ObjectClass", "top", "person" );
        serverEntry.put( "sn", "elecharny" );
        serverEntry.put( "cn", "Emmanuel Lecharny" );
    }

    
    /**
     * Test a move of a non existing entry (not a referral), with no referral 
     * in its ancestor.
     */
    @Test
    public void testMoveNotExistingSuperiorNoReferralAncestor() throws Exception
    {
        try
        {
            WWCtx.rename( "cn=Emmanuel Lecharny,o=MNN", "cn=Emmanuel Lecharny,o=PNN" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test a move of a non existing entry having some referral ancestor in its ancestor,
     * using JNDI throw.
     */
    @Test
    public void testMoveNotExistingSuperiorReferralAncestorJNDIThrow() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "throw" );
            MNNCtx.rename( "cn=Emmanuel Lecharny,ou=Roles", "cn=Emmanuel Lecharny,o=PNN,c=WW,ou=system" );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Emmanuel%20Lecharny,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }
    
    
    /**
     * Test a move of a non existing entry having some referral ancestor in its ancestor,
     * using JNDI ignore.
     */
    @Test
    public void testMoveNotExistingSuperiorReferralAncestorJNDIIgnore() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );
            MNNCtx.rename( "cn=Emmanuel Lecharny,ou=Roles", "cn=Emmanuel Lecharny,o=PNN,c=WW,ou=system" );
            fail();
        }
        catch ( PartialResultException re )
        {
            assertTrue( true );
        }
    }
    
    
    /**
     * Test a move of a non existing entry having some referral ancestor in its ancestor,
     * using the Core API without ManageDsaIt flag
     */
    @Test
    public void testMoveNotExistingSuperiorReferralAncestorCoreAPIWithoutManageDsaIt() throws Exception
    {
        CoreSession coreSession = service.getAdminSession();
        LdapDN dn = new LdapDN( "cn=Emmanuel Lecharny,ou=Roles,o=MNN,c=WW,ou=system" );
        LdapDN newParent = new LdapDN( "cn=Emmanuel Lecharny,o=PNN,c=WW,ou=system" ); 
        
        try
        {
            coreSession.move( dn, newParent, false );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Emmanuel%20Lecharny,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }
    
    
    /**
     * Test a move of a non existing entry having some referral ancestor in its ancestor,
     * using the Core API with ManageDsaIt flag
     */
    @Test
    public void testMoveNotExistingSuperiorReferralAncestorCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession coreSession = service.getAdminSession();
        LdapDN dn = new LdapDN( "cn=Emmanuel Lecharny,ou=Roles,o=MNN,c=WW,ou=system" );
        LdapDN newParent = new LdapDN( "cn=Emmanuel Lecharny,o=PNN,c=WW,ou=system" ); 
        
        try
        {
            coreSession.move( dn, newParent, true );
            fail();
        }
        catch ( PartialResultException re )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a move of an existing entry (not a referral), with no referral 
     * in its ancestor.
     */
    @Test
    public void testMoveExistingSuperiorNoReferralAncestor() throws Exception
    {
        // First check that the object exists
        Object moved = MNNCtx.lookup( "cn=Alex" );
        assertNotNull( moved );

        // and that the target entry is not present
        try
        {
            moved = PNNCtx.lookup( "cn=Alex" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }

        WWCtx.rename( "cn=Alex,o=MNN", "cn=Alex,o=PNN" );
        
        // Check that the entry has been moved
        moved = PNNCtx.lookup( "cn=Alex" );
        assertNotNull( moved );

        // and that the original entry is not present anymore
        try
        {
            moved = MNNCtx.lookup( "cn=Alex" );
            fail();
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a move of an existing entry (not a referral), to a new superior
     * being a referral
     */
    @Test
    public void testMoveExistingSuperiorIsReferral() throws Exception
    {
        try
        {
            MNNCtx.rename( "cn=Alex", "cn=Alex,ou=Roles" );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
            assertEquals( ResultCodeEnum.AFFECTS_MULTIPLE_DSAS, ((LdapNamingException)ne).getResultCode() );
        }
    }

    
    /**
     * Test a move of an existing entry (not a referral), to a new superior
     * having a referral ancestor
     */
    @Test
    public void testMoveExistingSuperiorHasReferralAncestor() throws Exception
    {
        try
        {
            MNNCtx.rename( "cn=Alex", "cn=Alex,ou=apache,ou=Roles" );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
            assertEquals( ResultCodeEnum.AFFECTS_MULTIPLE_DSAS, ((LdapNamingException)ne).getResultCode() );
        }
    }

    
    /**
     * Test a move of an existing entry with a referral in its ancestot, 
     * to a new superior, using JNDI throw
     */
    @Test
    public void testMoveEntryWithReferralAncestorJNDIThrow() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "throw" );
            MNNCtx.rename( "cn=Alex,ou=roles", "cn=Alex,ou=People" );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Alex,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }

    
    /**
     * Test a move of an existing entry with a referral in its ancestot, 
     * to a new superior, using JNDI ignore
     */
    @Test
    public void testMoveEntryWithReferralAncestorJNDIIgnore() throws Exception
    {
        try
        {
            MNNCtx.addToEnvironment( DirContext.REFERRAL, "ignore" );
            MNNCtx.rename( "cn=Alex,ou=roles", "cn=Alex,ou=People" );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }

    
    /**
     * Test a move of an existing entry with a referral in its ancestot, 
     * to a new superior, using CoreAPI without the ManageDsaIT flag
     */
    @Test
    public void testMoveEntryWithReferralAncestorCoreAPIWithoutManageDsaIt() throws Exception
    {
        CoreSession coreSession = service.getAdminSession();
        LdapDN orig = new LdapDN( "cn=Alex,ou=roles,o=MNN,c=WW,ou=system" );
        LdapDN dest = new LdapDN( "cn=Alex,ou=People,o=MNN,c=WW,ou=system" );
        
        try
        {
            coreSession.move( orig, dest, false );
            fail();
        }
        catch ( ReferralException re )
        {
            int nbRefs = 0;
            Set<String> expectedRefs = new HashSet<String>();
            expectedRefs.add( "ldap://hostd/cn=Alex,ou=Roles,dc=apache,dc=org" );
            
            do 
            {
                String ref = (String)re.getReferralInfo();
                
                assertTrue( expectedRefs.contains( ref ) );
                nbRefs ++;
            }
            while ( re.skipReferral() );
            
            assertEquals( 1, nbRefs );
        }
    }


    /**
     * Test a move of an existing entry with a referral in its ancestot, 
     * to a new superior, using CoreAPI with the ManageDsaIT flag
     */
    @Test
    public void testMoveEntryWithReferralAncestorCoreAPIWithManageDsaIt() throws Exception
    {
        CoreSession coreSession = service.getAdminSession();
        LdapDN orig = new LdapDN( "cn=Alex,ou=roles,o=MNN,c=WW,ou=system" );
        LdapDN dest = new LdapDN( "cn=Alex,ou=People,o=MNN,c=WW,ou=system" );
        
        try
        {
            coreSession.move( orig, dest, true );
            fail();
        }
        catch ( PartialResultException pre )
        {
            assertTrue( true );
        }
    }
}
