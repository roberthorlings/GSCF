<%
/**
 * Templates page
 *
 * @author  Jeroen Wesbeek
 * @since   20100303
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
<wizard:pageContent>

	<span class="bigtext">What would you like to do?</span>

	<wizard:ajaxButton name="next" class="bigbutton" value="Create a new study" alt="Create a new study" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" />

	<wizard:ajaxButton name="modify" class="bigbutton" value="Modify an existing study" alt="Modify an existing study" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" />

	<span class="info">
		<span class="title">Create a new study via the step-by-step interface</span>
		This web interface will guide you through the total incorporation of your study in several steps:
		<ul>
			<li>Include all general study information</li>
			<li>Include all subject specific information</li>
			<li>Include all information on all variables of the study (treatment, challenges, sampling etc.)</li>
			<li>Confirmation of all information</li>
		</ul>
		It is possible to go back and forth in the tool, without losing information. Definitive storage will only occur after the confirmation step.
		<span class="title">Create a new study via the spreadsheet importer</span>
		You can do this by choosing Studies > Import data in the menu.
		This part of the applications will help you to upload large studies. This import function can be used for all study components (study, subject and event information) at once.
	    <span class="title">Modify an existing study</span>
		Only study owners can modify a their own studies. This part of the application can be used to extend the study information, for instance with new measurements.
	</span>

	<g:if env="development">
	<span class="info">
		<span class="known">Known issues</span>
		<ul>
			<li>navigating away from the wizard will result in loss of work. While you are currently warned when
			    clicking links outside of the wizard, this problem still exists when clicking 'refresh' or the
				back / forward buttons</li>
		</ul>
	</span>
	<!--g:render template="pages/demo"//-->
	</g:if>

</wizard:pageContent>