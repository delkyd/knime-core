/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   20 Feb 2017 (albrecht): created
 */
package org.knime.core.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.web.WebResourceLocator;
import org.knime.core.node.web.WebResourceLocator.WebResourceType;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WizardExecutionController;
import org.knime.core.node.workflow.WizardExecutionController.WizardPageContent;
import org.knime.js.core.JSONViewContent;
import org.knime.js.core.JSONWebNode;
import org.knime.js.core.JSONWebNodePage;
import org.knime.js.core.JSONWebNodePageConfiguration;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.selections.json.JSONSelectionTranslator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public class WizardPageManager {

    private final WizardExecutionController m_wec;

    /**
     *
     */
    public WizardPageManager(final WizardExecutionController wizardExecutionController) {
        m_wec = wizardExecutionController;
    }

    /**
     * Creates a JSON string containing a wizard page from a given node id
     * @param containerNodeID the node id to create the wizard page string for
     * @return a JSON string containing the wizard page
     * @throws IOException if the layout of the wizard page can not be generated
     * @throws JsonProcessingException on serialization
     */
    public String createWizardPage(final NodeID containerNodeID) throws IOException, JsonProcessingException {
        WizardPageContent page = m_wec.getWizardPage(containerNodeID);
        // process layout
        JSONLayoutPage layout = new JSONLayoutPage();
        try {
            String lString = page.getLayoutInfo();
            if (lString != null && !lString.isEmpty()) {
                layout = getJSONLayoutFromSubnode(page.getPageNodeID(), page.getLayoutInfo());
            }
        } catch (IOException e) {
            throw new IOException("Layout for page could not be generated: " + e.getMessage(), e);
        }

        // process selection translators
        List<JSONSelectionTranslator> selectionTranslators = new ArrayList<JSONSelectionTranslator>();
        if (page.getHiLiteTranslators() != null) {
            for (HiLiteTranslator hiLiteTranslator : page.getHiLiteTranslators()) {
                if (hiLiteTranslator != null) {
                    selectionTranslators.add(new JSONSelectionTranslator(hiLiteTranslator));
                }
            }
        }
        if (page.getHiliteManagers() != null) {
            for (HiLiteManager hiLiteManager : page.getHiliteManagers()) {
                if (hiLiteManager != null) {
                    selectionTranslators.add(new JSONSelectionTranslator(hiLiteManager));
                }
            }
        }
        if (selectionTranslators.size() < 1) {
            selectionTranslators = null;
        }
        JSONWebNodePageConfiguration pageConfig = new JSONWebNodePageConfiguration(layout, null, selectionTranslators);

        Map<String, JSONWebNode> nodes = new HashMap<String, JSONWebNode>();
        for (@SuppressWarnings("rawtypes") Map.Entry<NodeIDSuffix, WizardNode> e : page.getPageMap().entrySet()) {
            WizardNode<?, ?> node = e.getValue();
            WebTemplate template =
                WizardExecutionController.getWebTemplateFromJSObjectID(node.getJavascriptObjectID());
            List<String> jsList = new ArrayList<String>();
            List<String> cssList = new ArrayList<String>();
            for (WebResourceLocator locator : template.getWebResources()) {
                if (locator.getType() == WebResourceType.JAVASCRIPT) {
                    jsList.add(locator.getRelativePathTarget());
                } else if (locator.getType() == WebResourceType.CSS) {
                    cssList.add(locator.getRelativePathTarget());
                }
            }
            JSONWebNode jsonNode = new JSONWebNode();
            jsonNode.setJavascriptLibraries(jsList);
            jsonNode.setStylesheets(cssList);
            jsonNode.setNamespace(template.getNamespace());
            jsonNode.setInitMethodName(template.getInitMethodName());
            jsonNode.setValidateMethodName(template.getValidateMethodName());
            jsonNode.setSetValidationErrorMethodName(template.getSetValidationErrorMethodName());
            jsonNode.setGetViewValueMethodName(template.getPullViewContentMethodName());
            jsonNode.setViewRepresentation((JSONViewContent)node.getViewRepresentation());
            jsonNode.setViewValue((JSONViewContent)node.getViewValue());
            nodes.put(e.getKey().toString(), jsonNode);
        }
        JSONWebNodePage jsonPage = new JSONWebNodePage(pageConfig, nodes);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        return mapper.writeValueAsString(jsonPage);
    }

    private JSONLayoutPage getJSONLayoutFromSubnode(final NodeIDSuffix pageID, final String layoutInfo) throws IOException {
        ObjectMapper mapper = JSONLayoutPage.getConfiguredVerboseObjectMapper();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        JSONLayoutPage page = reader.readValue(layoutInfo);
        if (page != null && page.getRows() != null) {
            for (JSONLayoutRow row : page.getRows()) {
                setNodeIDInContent(row, pageID);
            }
        }
        return page;
    }

    private void setNodeIDInContent(final JSONLayoutContent content, final NodeIDSuffix pageID) {
        if (content instanceof JSONLayoutRow) {
            for (JSONLayoutColumn col : ((JSONLayoutRow)content).getColumns()) {
                for (JSONLayoutContent subContent : col.getContent()) {
                    setNodeIDInContent(subContent, pageID);
                }
            }
        } else if (content instanceof JSONLayoutViewContent) {
            JSONLayoutViewContent view = (JSONLayoutViewContent)content;
            String nodeIDString = view.getNodeID();
            if (pageID != null) {
                NodeIDSuffix layoutNodeID = pageID.createChild(Integer.parseInt(view.getNodeID()));
                nodeIDString = layoutNodeID.toString();
            }
            view.setNodeID(nodeIDString);
        }
    }

    public String applyViewValues(final String viewValues, final NodeID containerNodeId) {
        //TODO: fill me
        return null;
    }

}
