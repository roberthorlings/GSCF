<%
/**
 * Wizard navigational buttons
 *
 * @author  Jeroen Wesbeek
 * @since   20100113
 * @package wizard
 * @see     dbnp.studycapturing.WizardController
 *
 * Revision information:
 * $Rev$
 * $Author$
 * $Date$
 */
%>
    <div class="navigation">
      <g:if test="${page>1 && page<pages.size}"><wizard:ajaxButton name="previous" value="&laquo; prev" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" class="prevnext" /></g:if>
	  <g:if test="${cancel}">
		[ <wizard:ajaxButton name="cancel" value="cancel" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" class="prevnext" /> ]
	  </g:if>
      <g:elseif test="${page>1 && page<pages.size}">
		  |		  
	  </g:elseif>
	  <g:if test="${page<pages.size}"><wizard:ajaxButton name="next" value="next &raquo;" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" class="prevnext" /></g:if>
    </div>

<script type="text/javascript">
function refreshWebFlow() {
	<wizard:ajaxSubmitJs name="refresh" url="[controller:'wizard',action:'pages']" update="[success:'wizardPage',failure:'wizardError']" afterSuccess="onWizardPage()" class="prevnext" />
}
</script>
