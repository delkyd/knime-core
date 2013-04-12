/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.node.workflow.bug4175_endless_quickform;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4175EndlessQuickform extends WorkflowTestCase {

    private NodeID m_quickFormBoolean;
    private NodeID m_tableViewEnd;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_quickFormBoolean = new NodeID(baseID, 1);
        m_tableViewEnd = new NodeID(baseID, 14);
    }

    public void testExecuteFlow() throws Exception {
        checkState(m_quickFormBoolean, State.CONFIGURED);
        checkState(m_tableViewEnd, State.IDLE);
        executeAndWait(m_tableViewEnd);
        checkState(m_tableViewEnd, State.EXECUTED);
    }
    
    public void testStepExecute() throws Exception {
        WorkflowManager m = getManager();
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        WorkflowManager waitingWFM = m.findNextWaitingWorkflowManager(
                QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        assertSame(m, waitingWFM);
        Map<NodeID, QuickFormInputNode> waitingNodes = 
                waitingWFM.findWaitingNodes(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        assertEquals(waitingNodes.size(), 1);
        QuickFormInputNode booleanIn = waitingNodes.get(m_quickFormBoolean);
        assertNotNull(booleanIn);
        waitingWFM.executeUpToHere(m_quickFormBoolean);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        checkState(m_quickFormBoolean, State.EXECUTED);
        checkState(m_tableViewEnd, State.IDLE);
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        checkState(m_tableViewEnd, State.EXECUTED);
    }
    
    public void testStepExecuteAfterExecuteAll() throws Exception {
        executeAllAndWait();
        checkState(m_tableViewEnd, State.EXECUTED);
        WorkflowManager m = getManager();
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        checkState(m_tableViewEnd, State.EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
