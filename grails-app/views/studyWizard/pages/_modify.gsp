<%
/**
 * Load study to modify page
 *
 * @author  Jeroen Wesbeek
 * @since   20100414
 * @package wizard
 * @see     dbnp.studycapturing.WizardTagLib::previousNext
 * @see     dbnp.studycapturing.WizardController
 *
 * Revision information:
 * $Rev$
 * $Author$
 * $Date$
 */
%>
<af:page>
	<span class="info">
		<span class="title">Edit a study</span>
		Select the study you would like to modify.
	</span>

	<af:studyElement name="studyid" description="Study" error="study" value="">
		The study you would like to load and edit
	</af:studyElement>
</af:page>