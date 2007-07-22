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
package org.apache.directory.server.schema.bootstrap;


import javax.naming.NamingException;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.shared.ldap.schema.syntax.JavaByteSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JavaIntegerSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.JavaShortSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;



/**
 * A producer of SyntaxChecker objects for the apache schema.  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheSyntaxCheckerProducer extends AbstractBootstrapProducer
{
    public ApacheSyntaxCheckerProducer()
    {
        super( ProducerTypeEnum.SYNTAX_CHECKER_PRODUCER );
    }


    // ------------------------------------------------------------------------
    // BootstrapProducer Methods
    // ------------------------------------------------------------------------


    /**
     * @see BootstrapProducer#produce(Registries, ProducerCallback)
     */
    public void produce( Registries registries, ProducerCallback cb )
        throws NamingException
    {
        SyntaxChecker checker = null;
        
        checker = new JavaByteSyntaxChecker();
        cb.schemaObjectProduced( this, checker.getSyntaxOid(), checker );
        
        checker = new JavaShortSyntaxChecker();
        cb.schemaObjectProduced( this, checker.getSyntaxOid(), checker );
        
        checker = new JavaIntegerSyntaxChecker();
        cb.schemaObjectProduced( this, checker.getSyntaxOid(), checker );
        
        checker = new JavaIntegerSyntaxChecker();
        cb.schemaObjectProduced( this, checker.getSyntaxOid(), checker );
    }
}
