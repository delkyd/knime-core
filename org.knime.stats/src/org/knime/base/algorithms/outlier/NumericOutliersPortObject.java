/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Feb 23, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.algorithms.outlier;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.base.algorithms.outlier.options.NumericOutliersDetectionOption;
import org.knime.base.algorithms.outlier.options.NumericOutliersReplacementStrategy;
import org.knime.base.algorithms.outlier.options.NumericOutliersTreatmentOption;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

/**
 * Port object that is passed to outlier apply node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class NumericOutliersPortObject extends AbstractSimplePortObject {

    /** @noreference This class is not intended to be referenced by clients. */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<NumericOutliersPortObject> {
    }

    /** Convenience accessor for the port type. */
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(NumericOutliersPortObject.class);

    /** Config key of the summary text. */
    private static final String CFG_SUMMARY = "summary";

    /** Config key for the group column types. */
    private static final String CFG_GROUP_COL_TYPES = "group-col-types";

    /** Config key of the outlier treatment. */
    private static final String CFG_OUTLIER_TREATMENT = "outlier-treatment";

    /** Config key of the outlier replacement strategy. */
    private static final String CFG_OUTLIER_REPLACEMENT = "replacement-strategy";

    /** Config key of the outlier detection option. */
    private static final String CFG_DETECTION_OPTION = "detection-option";

    /** Config key of the domain policy. */
    private static final String CFG_DOMAIN_POLICY = "update-domain";

    /** Config key of the outlier treatment policy. */
    private static final String CFG_REVISER = "outlier-treatment";

    /** Config key of the permitted intervals. */
    private static final String CFG_INTERVALS = "permitted intervals";

    /** The summary (tooltip) text. */
    private String m_summary;

    /** The outlier model. */
    private NumericOutliersModel m_outlierModel;

    /** The data types of the group columns . */
    private DataType[] m_groupColTypes;

    /** The outlier treatment option. */
    private NumericOutliersTreatmentOption m_treatmentOption;

    /** The outlier replacement strategy. */
    private NumericOutliersReplacementStrategy m_repStrategy;

    /** The outlier detection option. */
    private NumericOutliersDetectionOption m_detectionOption;

    /** Flag indiciation whether the domain needs to be updated. */
    private boolean m_updateDomain;

    /** Empty constructor required by super class, should not be used. */
    public NumericOutliersPortObject() {
    }

    /**
     * Create new port object given the arguments.
     *
     * @param summary the tooltip
     * @param inSpec the in spec
     * @param outlierModel ther permitted intervals table
     * @param reviser the outlier reviser
     */
    NumericOutliersPortObject(final String summary, final DataTableSpec inSpec, final NumericOutliersModel outlierModel,
        final NumericOutliersReviser reviser) {
        // store the summary
        m_summary = summary;

        // store the permited intervals model
        m_outlierModel = outlierModel;

        // store the group col types
        m_groupColTypes = extractTypes(inSpec, m_outlierModel.getGroupColNames());

        // store the reviser settings
        m_treatmentOption = reviser.getTreatmentOption();
        m_repStrategy = reviser.getReplacementStrategy();
        m_detectionOption = reviser.getRestrictionOption();
        m_updateDomain = reviser.updateDomain();
    }

    /**
     * Returns the proper instance of the outlier reviser builder
     *
     * @return properly instantiated outlier reviser builder
     */
    public NumericOutliersReviser.Builder getOutRevBuilder() {
        return new NumericOutliersReviser.Builder()//
            .setTreatmentOption(m_treatmentOption)//
            .setReplacementStrategy(m_repStrategy)//
            .setDetectionOption(m_detectionOption)//
            .updateDomain(m_updateDomain);
    }

    /**
     * Returns the outlier model only containing columns that that exist in and are compatible with the in spec.
     *
     * @param inSpec the in spec of the table whose outlier have to be treated
     * @return the filtered outlier model
     */
    public NumericOutliersModel getOutlierModel(final DataTableSpec inSpec) {
        // remove all entries related to outlier columns not existent in the input spec
        m_outlierModel.dropOutliers(Arrays.stream(m_outlierModel.getOutlierColNames())
            .filter(s -> !inSpec.containsName(s) || !inSpec.getColumnSpec(s).getType().isCompatible(DoubleValue.class))
            .collect(Collectors.toList()));
        return m_outlierModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NumericOutliersPortObjectSpec getSpec() {
        return getPortSpec(m_groupColTypes, m_outlierModel.getGroupColNames(), m_outlierModel.getOutlierColNames());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        return m_summary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
        model.addString(CFG_SUMMARY, m_summary);
        model.addDataTypeArray(CFG_GROUP_COL_TYPES, m_groupColTypes);
        saveReviserSettings(model.addModelContent(CFG_REVISER));
        m_outlierModel.saveModel(model.addModelContent(CFG_INTERVALS));
    }

    /**
     * Saves the reviser to the model content.
     *
     * @param model the model to save to
     */
    private void saveReviserSettings(final ModelContentWO model) {
        model.addString(CFG_OUTLIER_TREATMENT, m_treatmentOption.toString());
        model.addString(CFG_OUTLIER_REPLACEMENT, m_repStrategy.toString());
        model.addString(CFG_DETECTION_OPTION, m_detectionOption.toString());
        model.addBoolean(CFG_DOMAIN_POLICY, m_updateDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        // load the summary
        m_summary = model.getString(CFG_SUMMARY);

        // load the types
        m_groupColTypes = model.getDataTypeArray(CFG_GROUP_COL_TYPES);

        // initialize the reviser builder
        final ModelContentRO reviserModel = model.getModelContent(CFG_REVISER);

        // load the reviser settings
        m_treatmentOption = NumericOutliersTreatmentOption.getEnum(reviserModel.getString(CFG_OUTLIER_TREATMENT));
        m_repStrategy = NumericOutliersReplacementStrategy.getEnum(reviserModel.getString(CFG_OUTLIER_REPLACEMENT));
        m_detectionOption = NumericOutliersDetectionOption.getEnum(reviserModel.getString(CFG_DETECTION_OPTION));
        m_updateDomain = reviserModel.getBoolean(CFG_DOMAIN_POLICY);

        // initialize the permitted intervals model
        m_outlierModel = NumericOutliersModel.loadInstance(model.getModelContent(CFG_INTERVALS));
    }

    /**
     * Returns the oulier port spec.
     *
     * @param inSpec the in spec
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @return the outlier port spec
     */
    static NumericOutliersPortObjectSpec getPortSpec(final DataTableSpec inSpec, final String[] groupColNames,
        final String[] outlierColNames) {
        return getPortSpec(extractTypes(inSpec, groupColNames), groupColNames, outlierColNames);
    }

    /**
     * Returns the oulier port spec.
     *
     * @param groupColTypes the data types of the group column
     * @param groupColNames the group column names
     * @param outlierColNames the outlier column names
     * @return the outlier port spec
     */
    private static NumericOutliersPortObjectSpec getPortSpec(final DataType[] groupColTypes, final String[] groupColNames,
        final String[] outlierColNames) {
        return new NumericOutliersPortObjectSpec(groupColTypes, groupColNames, outlierColNames);
    }

    /**
     * Extracts the data types from the given in spec for the given column names.
     *
     * @param inSpec the spec holding the data types
     * @param columnNames the column names for which the data types need to be extracted
     * @return the data types for the provided column names
     */
    private static DataType[] extractTypes(final DataTableSpec inSpec, final String[] columnNames) {
        return Arrays.stream(columnNames)//
            .map(s -> inSpec.getColumnSpec(s).getType())//
            .toArray(DataType[]::new);
    }

}
