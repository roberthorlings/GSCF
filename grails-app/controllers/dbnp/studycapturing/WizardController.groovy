package dbnp.studycapturing

import dbnp.data.*

// Grails convertors is imported in order to create JSON objects
import grails.converters.*


/**
 * Wizard Controler
 *
 * The wizard controller handles the handeling of pages and data flow
 * through the study capturing wizard.
 *
 * @author Jeroen Wesbeek
 * @since 20100107
 * @package studycapturing
 *
 * Revision information:
 * $Rev$
 * $Author$
 * $Date$
 */
class WizardController {
	/**
	 * index method, redirect to the webflow
	 * @void
	 */
	def index = {
		def jump = [:]

		// allow quickjumps to:
		//	edit a study	: /wizard?jump=edit&id=1
		//	create a study	: /wizard?jump=create
		if (params.get('jump')) {
			switch (params.get('jump')) {
				case 'create':
					jump = [
					    action: 'create'
					]
					break
				case 'edit':
					jump = [
					    action	: 'edit',
						id		: params.get('id')
					]
					break
				default:
					break
			}
		}

		// store in session
		session.jump = jump

		/**
		 * Do you believe it in your head?
		 * I can go with the flow
		 * Don't say it doesn't matter (with the flow) matter anymore
		 * I can go with the flow (I can go)
		 * Do you believe it in your head?
		 */
		redirect(action: 'pages')
	}

	/**
	 * WebFlow definition
	 * @see http://grails.org/WebFlow
	 * @void
	 */
	def pagesFlow = {
		// start the flow
		onStart {
			// define flow variables
			flow.page = 0
			flow.pages = [
				//[title: 'Templates'],			// templates
				[title: 'Start'],				// load or create a study
				[title: 'Study'],				// study
				[title: 'Subjects'],			// subjects
				[title: 'Events'],				// events and event grouping
				[title: 'Groups'],				// groups
				[title: 'Samples'],				// samples
				[title: 'Confirmation'],		// confirmation page
				[title: 'Done']					// finish page
			]
			flow.jump = session.jump
			success()
		}

		// render the main wizard page which immediately
		// triggers the 'next' action (hence, the main
		// page dynamically renders the study template
		// and makes the flow jump to the study logic)
		mainPage {
			render(view: "/wizard/index")
			onRender {
				flow.page = 1
				success()
			}
			on("next").to "handleJump"
		}

		// handle the jump parameter
		//
		// I came to get down [2x]
		// So get out your seats and jump around
		// Jump around [3x]
		// Jump up Jump up and get down
		// Jump [18x]
		handleJump {
			action {
				if (flow.jump && flow.jump.action == 'edit' && flow.jump.id) {
					// load study
					if (this.loadStudy(flow, flash, [studyid:flow.jump.id])) {
						toStudyPage()
					} else {
						toStartPage()
					}
				} else if (flow.jump && flow.jump.action == 'create') {
					toStudyPage()
				} else {
					toStartPage()
				}
			}
			on("toStartPage").to "start"
			on("toStudyPage").to "study"
		}

		// create or modify a study
		start {
			render(view: "_start")
			onRender {
				flow.page = 1
				success()
			}
			on("next") {
				// clean the flow scope
				flow.remove('study')
				flow.remove('subjects')
				flow.remove('subjectTemplates')
				flow.remove('event')
				flow.remove('events')
				flow.remove('eventGroups')
				flow.remove('eventTemplates')
				flow.remove('samples')
				flow.remove('sampleTemplates')

				// set 'quicksave' variable to false
				flow.quickSave = false
			}.to "study"
			on("modify").to "modify"
			on("import").to "redirectToImport"
		}

		// redirect to the import wizard
		redirectToImport {
			render(view: "_redirect")
			onRender {
				flash.uri = "/importer/index"
			}
			on("next").to "start"
		}

		// load a study to modify
		modify {
			render(view: "_modify")
			onRender {
				flow.page = 1
				flash.cancel = true
				success()
			}
			on("cancel") {
				flow.study = null

				success()
			}.to "start"
			on("next") {
				// load study
				if (this.loadStudy(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "study"
		}

		// render and handle the study page
		study {
			render(view: "_study")
			onRender {
				flow.page = 2
				success()
			}
			on("refresh") {
				println ".refreshing...."
				flash.values = params

				// handle study data
				this.handleStudy(flow, flash, params)

				// force refresh of the template
				if (flow.study.template) {
					flow.study.template.refresh()
				}

				// remove errors as we don't want any warnings now
				flash.errors = [:]

				success()
			}.to "study"
            on("switchTemplate") {
				flash.values = params

				// handle study data
				this.handleStudy(flow, flash, params)

				// force refresh of the template
				if (flow.study.template) {
					flow.study.template.refresh()
				}

				// remove errors as we don't want any warnings now
				flash.errors = [:]

				success()
			}.to "study"
			on("previous") {
				flash.errors = [:]

				// handle the study
				this.handleStudy(flow, flash, params)

				// reset errors
				flash.errors = [:]

				success()
			}.to "start"
			on("next") {
				flash.errors = [:]

				if (this.handleStudy(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "subjects"
			on("quickSave") {
				flash.errors = [:]

				if (this.handleStudy(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "waitForSave"
		}

		// render and handle subjects page
		subjects {
			render(view: "_subjects")
			onRender {
				flow.page = 3

				if (!flow.subjects) {
					flow.subjects = [:]
					flow.subjectTemplates = [:]
				}

				if (!flash.values) flash.values = [addNumber:1]

				success()
			}
			on("refresh") {
				flash.values = params

				// refresh templates
				flow.subjectTemplates.each() {
					it.value.template.refresh()
				}

				success()
			}.to "subjects"
			on("add") {
				flash.errors = [:]

				// handle subjects
				this.handleSubjects(flow, flash, params)

				flash.errors = [:]
				flash.values = params

				def speciesTerm = Term.findByName(params.species)
				def subjectTemplateName = params.get('template')
				def subjectTemplate = Template.findByName(subjectTemplateName)

				// got a species and a subjectTemplate?
				if (speciesTerm && subjectTemplate) {
					// add this subject template to the subject template array
					if (!flow.subjectTemplates[subjectTemplateName]) {
						flow.subjectTemplates[subjectTemplateName] = [
							name: subjectTemplateName,
							template: subjectTemplate,
							subjects: [:]
						]
					}

					// add x subjects of species y
					(params.addNumber as int).times {
						def increment = (flow.subjects.size()) ? (flow.subjects.keySet().max() + 1) : 0
						def subject = new Subject(
							name: 'Subject ' + (increment + 1),
							species: speciesTerm,
							template: subjectTemplate
						)

						// instantiate a new Subject
						flow.subjects[increment] = subject

						// add the subject to the study
						flow.study.addToSubjects( subject )

						// and remember the subject id with the template
						def subjectsSize = (flow.subjectTemplates[subjectTemplateName].subjects.size()) ? (flow.subjectTemplates[subjectTemplateName].subjects.keySet().max() + 1) : 0
						flow.subjectTemplates[subjectTemplateName].subjects[subjectsSize] = increment
					}

					success()
				} else {
					// add feedback
					if (!speciesTerm) this.appendErrorMap(['species': 'You need to select a species, or add one if it is not yet present'], flash.errors)
					if (!subjectTemplate) this.appendErrorMap(['template': 'You need to select a template, or add one if it is not yet present'], flash.errors)

					error()
				}
			}.to "subjects"
			on("delete") {
				// handle subjects
				this.handleSubjects(flow, flash, params)

				flash.errors = [:]
				def delete = params.get('do') as int

				// remove subject
				if (flow.subjects[ delete ] && flow.subjects[ delete ] instanceof Subject) {
					// from study as well
					flow.study.removeFromSubjects( flow.subjects[ delete ] )

					// remove subject from templates
					flow.subjectTemplates.each() { templateName, templateData ->
						templateData.subjects.values().remove( delete )
					}

					// remove templates that contain no subject
					flow.subjectTemplates.find{!it.value.subjects.size()}.each{ flow.subjectTemplates.remove( it.key ) }

					// remove subject altogether
					flow.subjects.remove( delete )
				}
			}.to "subjects"
			on("previous") {
				flash.errors = [:]

				// handle form data
				if (!this.handleSubjects(flow, flash, params)) {
					error()
				} else {
					success()
				}
			}.to "study"
			on("next") {
				flash.errors = [:]

				// check form data
				if (!this.handleSubjects(flow, flash, params)) {
					error()
				} else {
					success()
				}
			}.to "events"
			on("quickSave") {				
				flash.errors = [:]

				// check form data
				if (!this.handleSubjects(flow, flash, params)) {
					error()
				} else {
					success()
				}
			}.to "waitForSave"
		}

		// render events page
		events {
			render(view: "_events")
			onRender {
				flow.page = 4

				if (!flow.event) {
					flow.event			= new Event()
					flow.events			= [:]
					flow.eventGroups	= [ new EventGroup(name: 'Group 1') ]
					flow.eventTemplates	= [:]

					// add initial eventGroup to study
					flow.study.addToEventGroups( flow.eventGroups[ 0 ] )
				} else if (!flash.values) {
					// set flash.values.templateType based on the event instance
					flash.values = [:]
					flash.values.templateType = (flow.event instanceof Event) ? 'event' : 'sample'
				}
				success()
			}
			on("clear") {
				flow.remove('event')
				success()
			}.to "events"
			on("switchTemplate") {
				flash.values = params

				// handle study data
				this.handleEvents(flow, flash, params)

				// refresh event templates
				flow.eventTemplates.each() {
					it.value.template.refresh()
				}

				// refresh flow template
				if (flow.event.template) flow.event.template.refresh()

				// remove errors as we don't want any warnings now
				flash.errors = [:]
			}.to "events"
			on("refresh") {
				flash.values = params

				// handle study data
				this.handleEvents(flow, flash, params)

				// refresh templates
				flow.eventTemplates.each() {
					it.value.template.refresh()
				}

				// refresh flow template
				if (flow.event.template) flow.event.template.refresh()

				// remove errors as we don't want any warnings now
				flash.errors = [:]
			}.to "events"
			on("add") {
				flash.values			= params
				def eventTemplateName	= (params.get('eventType') == 'event') ? params.get('eventTemplate') : params.get('sampleTemplate')
				def eventTemplate		= Template.findByName(eventTemplateName)

				// handle study data
				this.handleEvents(flow, flash, params)

				// add event to study
				if (flow.event instanceof SamplingEvent) {
					flow.study.addToSamplingEvents( flow.event )
				} else {
					flow.study.addToEvents( flow.event )
				}

				// validate event object
				if (flow.event.validate()) {
					// add this event template to the event template array
					if (!flow.eventTemplates[ eventTemplateName ]) {
						flow.eventTemplates[ eventTemplateName ] = [
							name: eventTemplateName,
							template: eventTemplate,
							events: []
						]
					}

					// ...store it in the events map in the flow scope...
					def increment	= flow.events.size()
					flow.events[ increment ] = flow.event

					// ...add it to the study...
					/*
					if (newEvent instanceof SamplingEvent) {
						flow.study.addToSamplingEvents( newEvent )
					} else {
						flow.study.addToEvents( newEvent )
					}
					*/

					// ...and 'reset' the event object in the flow scope
					flow.event = new Event(template: flow.event.template)
					
					// remember the event id with the template
					def eventSize = flow.eventTemplates[ eventTemplateName ]['events'].size()
					flow.eventTemplates[ eventTemplateName ]['events'][ eventSize ] = increment

					success()
				} else {
					// it does not validate, remove event from study
					if (flow.event instanceof SamplingEvent) {
						flow.study.removeFromSamplingEvents(flow.event)
					} else {
						flow.study.removeFromEvents(flow.event)
					}
					
					// show error feedback
					flash.errors = [:]
					this.appendErrors(flow.event, flash.errors)
					error()
				}
			}.to "events"
			on("deleteEvent") {
				flash.values = params
				def delete = params.get('do') as int

				// handle event groupings
				this.handleEventGrouping(flow, flash, params)

				// remove event
				if (flow.events[ delete ] && flow.events[ delete ] instanceof Event) {
					// remove it from the study
					if ( flow.events[ delete ] instanceof SamplingEvent ) {
						flow.study.removeFromSamplingEvents( flow.events[ delete ] )
					} else {
						flow.study.removeFromEvents( flow.events[ delete ] )
					}

					// remove it from the map
					flow.events.remove(delete)
					flow.eventTemplates.each() { eventTemplate ->
						eventTemplate.value.events = eventTemplate.value.events.minus(delete)
					}

					// find eventTemplates without events
			        flow.eventTemplates.find { eventTemplate ->
						eventTemplate.value.events.size() < 1
					}.each() {
						// remove eventTemplate
						flow.eventTemplates.remove( it.value.name )
					}
				}

				success()
			}.to "events"
			on("addEventGroup") {
				flash.values = params
				
				// handle study data
				this.handleEvents(flow, flash, params)

				// handle event groupings
				this.handleEventGrouping(flow, flash, params)

				def increment = flow.eventGroups.size()
				def groupName = "Group " + (increment + 1)

				// check if group name exists
				def nameExists = true
				def u = 0

				// make sure a unique name is generated
				while (nameExists) {
					u++
					def count = 0
					
					flow.eventGroups.each() {
						if (it.name == groupName) {
							groupName = "Group " + (increment + 1) + "," + u
						} else {
							count++
						}
					}

					nameExists = !(count == flow.eventGroups.size())
				}

				// remember eventGroup
				flow.eventGroups[ increment ] = new EventGroup( name: groupName )

				// and add the group to the study
				flow.study.addToEventGroups( flow.eventGroups[ increment ] )

				success()
			}.to "events"
			on("deleteEventGroup") {
				flash.values = params
				def delete = params.get('do') as int

				// handle event groupings
				this.handleEventGrouping(flow, flash, params)

				// remove the group with this specific id
				if (flow.eventGroups[delete] && flow.eventGroups[delete] instanceof EventGroup) {
					// remove the eventGroup from the study
					flow.study.removeFromEventGroups( flow.eventGroups[ delete ] )

					// remove this eventGroup
					flow.eventGroups.remove(delete)
				}

				success()
			}.to "events"
			on("previous") {
				// handle event groupings
				this.handleEventGrouping(flow, flash, params)
			}.to "subjects"
			on("next") {
				flash.values = params
				flash.errors = [:]

				// handle study data
				/* turned off by request of Kees / Leny / Jildau
				if (!flow.eventTemplates.find { eventTemplate -> eventTemplate.value.template.entity == SamplingEvent }) {
					// append error map
					this.appendErrorMap(['events': 'You need to create at least one sampling event for your study'], flash.errors)
					error()						
				} else
				*/
				if (this.handleEvents(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "groups"
			on("quickSave") {
				flash.values = params
				flash.errors = [:]

				// handle study data
				/* turned off by request of Kees / Leny / Jildau
				if (!flow.eventTemplates.find { eventTemplate -> eventTemplate.value.template.entity == SamplingEvent }) {
					// append error map
					this.appendErrorMap(['events': 'You need to create at least one sampling event for your study'], flash.errors)
					error()
				} else
				*/
				if (this.handleEvents(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "waitForSave"
		}

		// groups page
		groups {
			render(view: "_groups")
			onRender {
				flow.page = 5
				success()
			}
			on("previous") {
				this.handleSubjectGrouping(flow, flash, params)
				success()
			}.to "events"
			on("next") {
				this.handleSubjectGrouping(flow, flash, params)
				flash.check = true
				success()
			}.to "samples"
			on("quickSave") {
				this.handleSubjectGrouping(flow, flash, params)
				success()
			}.to "waitForSave"
		}

		// sample 'previous' page with warning
		samplePrevious {
			render(view: "_samples_previous_warning")
			onRender {
				flow.page = 6

				// TEMPORARY FIX TO REMOVE ALL SAMPLES AND REGENERATE THEM
				// THEN USER BROWSED BACK
				println ".removing samples from study"

				// remove samples from study
				flow.samples.each() {
					flow.study.removeFromSamples(it.sample)
				}

				// remove samples from flow
				flow.remove('samples')
				// END FIX
			}
			on("next").to "samples"
			on("previous").to "groups"
		}

		// samples page
		samples {
			render(view: "_samples")
			onRender {
				flow.page = 6

				// iterate through eventGroups
				if (!flow.samples) {
					println ".generating samples"
					flow.samplesWithTemplate = 0
					flow.samples = []
					flow.sampleTemplates = [:]
					flow.eventGroups.each() { eventGroup ->
						// iterate through events
						eventGroup.samplingEvents.each() { samplingEvent ->
							def samplingEventName = this.ucwords(samplingEvent.template.name)

							// iterate through subjects
							eventGroup.subjects.each() { subject ->
								def sampleName = (this.ucwords(subject.name) + '_' + samplingEventName + '_' + new RelTime(samplingEvent.startTime).toString()).replaceAll("([ ]{1,})", "")
								def increment = flow.samples.size()

								flow.samples[increment] = [
									sample: new Sample(
										parent: flow.study,
										parentSubject: subject,
										parentEvent: samplingEvent,
										name: sampleName
									),
									name: sampleName,
									eventGroup: eventGroup,
									event: samplingEvent,
									subject: subject
								]

								// and add this sample to the study
								flow.study.addToSamples(flow.samples[increment].sample)
							}
						}
					}
				} else if (flash.check) {
					println "CHECKING SAMPLE CONSISTENCY"
					// check the consistency of the samples
					flow.samples.each() { sampleData ->
						println sampleData
						println sampleData.event.template
					}
				}

				success()
			}
			on("switchTemplate") {
				//println params
				handleSamples(flow, flash, params)

				// ignore errors
				flash.errors = [:]
				
				succes()
			}.to "samples"
			on("refresh") {
				println ".refresh ${flow.sampleTemplates.size()} sample templates (${flow.samples.size()} samples present)"

				// refresh templates
				flow.sampleTemplates.each() {
					println ".refresh template ["+it.value.name+"]"
					it.value.template.refresh()
					println "  --> fields: "+it.value.template.fields
				}

				// handle samples
				handleSamples(flow, flash, params)

				// ignore errors
				flash.errors = [:]

				success()
			}.to "samples"
			on("regenerate") {
				println ".removing 'samples' and 'sampleTemplates' from the flowscope, triggering regeneration of the samples..."
				flow.samples.each() {
					flow.study.removeFromSamples( it.sample )
				}
				flow.remove('samples')
				flow.remove('sampleTemplates')
				println flow.study.samples
				success()
			}.to "samples"
			on("previous") {
				// handle samples
				handleSamples(flow, flash, params)

				// ignore errors
				flash.errors = [:]

				success()
			}.to "samplePrevious"
			on("next") {
				flash.values = params
				flash.errors = [:]

// for now, development only!
if (grails.util.GrailsUtil.environment == "development") {
				// do all samples have a template assigned?
				if (flow.samplesWithTemplate < flow.samples.size()) {
					// handle samples
					this.handleSamples(flow, flash, params)

					// ignore errors
					flash.errors = [:]
					
					// add error
					this.appendErrorMap(['samples': 'you need to select a template for each sample'], flash.errors)

					error()
				} else if (this.handleSamples(flow, flash, params)) {
					success()
				} else {
					error()
				}
} else {
	success()
}
			}.to "confirm"
			on("quickSave") {
				// handle samples
				if (handleSamples(flow, flash, params)) {
					success()
				} else {
					error()
				}
			}.to "waitForSave"
		}

		// confirmation
		confirm {
			render(view: "_confirmation")
			onRender {
				flow.page = 7
			}
			on("toStudy").to "study"
			on("toSubjects").to "subjects"
			on("toEvents").to "events"
			on("toGroups").to "groups"
			on("previous").to "samples"
			on("next").to "waitForSave"
			on("quickSave").to "waitForSave"
		}

		waitForSave {
			render(view: "_wait")
			onRender {
				flow.page = 8
			}
			on("next").to "save"
		}

		// store all study data
		save {
			action {
				println "saving..."
				flash.errors = [:]

				// persist data to the database
				try {
					// user modifying?
					if (flow.quickSave && grails.util.GrailsUtil.environment == 'production') {
						// yes, not allowed on production as this results in data inconsistency
						println ".saving is not allowed in study edit wizard"
						throw new Exception("Saving is not allowed in the edit study wizard because this leads to data inconsistency in the database!")
					}

					// save study
					println ".saving study"
					if (!flow.study.save()) {
						this.appendErrors(flow.study, flash.errors)
						throw new Exception('error saving study')
					}
					println ".saved study "+flow.study+" (id: "+flow.study.id+")"

					success()
				} catch (Exception e) {
					// rollback
					this.appendErrorMap(['exception': e.toString() + ', see log for stacktrace' ], flash.errors)

					// stacktrace in flash scope
					flash.debug = e.getStackTrace()

					error()
				}
			}
			on("error").to "error"
			on(Exception).to "error"
			on("success").to "done"
		}

		// error storing data
		error {
			render(view: "_error")
			onRender {
				flow.page = 7
			}
			on("next").to "waitForSave"
			on("previous").to "samples"
		}

		// render finish page
		done {
			render(view: "_done")
			onRender {
				flow.page = 8
			}
			onEnd {
				// clean flow scope
				flow.clear()
			}
		}
	}

	/**
	 * load a study
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns boolean
	 */
	def loadStudy(flow, flash, params) {
		// load study
		try {
			// load study
			flow.study = (params.studyid) ? Study.findById( params.studyid ) : Study.findByTitle( params.study )

			// recreate subjects
			flow.subjects = [:]
			flow.subjectTemplates = [:]
			flow.study.subjects.each() { subject ->
				def subjectIncrement = flow.subjects.size()
				flow.subjects[subjectIncrement] = subject

				// add subject template?
				if (!flow.subjectTemplates[subject.template.name]) {
					flow.subjectTemplates[subject.template.name] = [
						name: subject.template.name,
						template: subject.template,
						subjects: [:]
					]
				}

				// reference subject in template
				flow.subjectTemplates[subject.template.name].subjects[flow.subjectTemplates[subject.template.name].subjects.size()] = subjectIncrement
			}

			// recreate events
			flow.events = [:]
			flow.eventGroups = []
			flow.eventTemplates = [:]
			flow.study.events.each() { event ->
				def eventIncrement = flow.events.size()
				flow.events[eventIncrement] = event

				// add event template?
				if (!flow.eventTemplates[event.template.name]) {
					flow.eventTemplates[event.template.name] = [
						name: event.template.name,
						template: event.template,
						events: new ArrayList()
					]
				}

				// reference event in template
				flow.eventTemplates[event.template.name].events[flow.eventTemplates[event.template.name].events.size()] = eventIncrement

				// set dummy event
				flow.event = event
			}

			// recreate sample events
			flow.study.samplingEvents.each() { event ->
				def eventIncrement = flow.events.size()
				flow.events[eventIncrement] = event

				// add event template?
				if (!flow.eventTemplates[event.template.name]) {
					flow.eventTemplates[event.template.name] = [
						name: event.template.name,
						template: event.template,
						events: new ArrayList()
					]
				}

				// reference event in template
				flow.eventTemplates[event.template.name].events[flow.eventTemplates[event.template.name].events.size()] = eventIncrement

				// set dummy event
				flow.event = event
			}

			// recreate eventGroups
			flow.study.eventGroups.each() { eventGroup ->
				flow.eventGroups[flow.eventGroups.size()] = eventGroup
			}

			// set 'quicksave' variable
			flow.quickSave = true

			return true
		} catch (Exception e) {
			// rollback
			this.appendErrorMap(['exception': e.toString() + ', see log for stacktrace'], flash.errors)

			return false
		}
	}

	/**
	 * re-usable code for handling study form data in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns boolean
	 */
	def handleStudy(flow, flash, params) {
		// create study instance if we have none
		if (!flow.study) flow.study = new Study()

		// create date instance from date string?
		// @see WizardTagLibrary::dateElement{...}
		if (params.get('startDate')) {
			params.startDate = new Date().parse("d/M/yyyy", params.get('startDate').toString())
		} else {
			params.remove('startDate')
		}

		// if a template is selected, get template instance
		def template = params.remove('template')
		if (template instanceof String && template.size() > 0) {
			flow.study.template = Template.findByName(template)
		} else if (template instanceof Template) {
			flow.study.template = template
		}

		// iterate through fields
		if (flow.study.template) {
			flow.study.giveFields().each() {
				flow.study.setFieldValue(it.name, params.get(it.escapedName()))
			}
		}

		// handle Publications and Contacts
		handlePublications(flow, flash, params)
		handleContacts(flow, flash, params)

		// validate study
		if (flow.study.validate()) {
			return true
		} else {
			// validation failed, feedback errors
			flash.errors = [:]
			this.appendErrors(flow.study, flash.errors)
			return false
		}
	}

	/**
	 * re-usable code for handling publications form data in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @returns boolean
	 */
	def handlePublications(flow, flash, params) {
		// create study instance if we have none
		if (!flow.study) flow.study = new Study()
		if (!flow.study.publications) flow.study.publications = []

		// Check the ids of the pubblications that should be attached
		// to this study. If they are already attached, keep 'm. If
		// studies are attached that are not in the selected (i.e. the
		// user deleted them), remove them
		def publicationIDs = params.get('publication_ids')
		if (publicationIDs) {
			// Find the individual IDs and make integers
			publicationIDs = publicationIDs.split(',').collect { Integer.parseInt(it, 10) }

			// First remove the publication that are not present in the array
			flow.study.publications.removeAll { publication -> !publicationIDs.find { id -> id == publication.id } }

			// Add those publications not yet present in the database
			publicationIDs.each { id ->
				if (!flow.study.publications.find { publication -> id == publication.id }) {
					def publication = Publication.get(id)
					if (publication) {
						flow.study.addToPublications(publication)
					} else {
						println('.publication with ID ' + id + ' not found in database.')
					}
				}
			}

		} else {
			println('.no publications selected.')
			flow.study.publications.clear()
		}

	}

	/**
	 * re-usable code for handling contacts form data in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleContacts(flow, flash, params) {
		// create study instance if we have none
		if (!flow.study) flow.study = new Study()
		if (!flow.study.persons) flow.study.persons = []

		// Check the ids of the contacts that should be attached
		// to this study. If they are already attached, keep 'm. If
		// studies are attached that are not in the selected (i.e. the
		// user deleted them), remove them

		// Contacts are saved as [person_id]-[role_id]
		def contactIDs = params.get('contacts_ids')
		if (contactIDs) {
			// Find the individual IDs and make integers
			contactIDs = contactIDs.split(',').collect {
				def parts = it.split('-')
				return [person: Integer.parseInt(parts[0]), role: Integer.parseInt(parts[1])]
			}

			// First remove the contacts that are not present in the array
			flow.study.persons.removeAll {
				studyperson -> !contactIDs.find { ids -> (ids.person == studyperson.person.id) && (ids.role == studyperson.role.id) }
			}

			// Add those contacts not yet present in the database
			contactIDs.each { ids ->
				if (!flow.study.persons.find { studyperson -> (ids.person == studyperson.person.id) && (ids.role == studyperson.role.id) }) {
					def person = Person.get(ids.person)
					def role = PersonRole.get(ids.role)
					if (person && role) {
						// Find a studyperson object with these parameters
						def studyPerson = StudyPerson.findAll().find { studyperson -> studyperson.person.id == person.id && studyperson.role.id == role.id }

						// If if does not yet exist, save the example
						if (!studyPerson) {
							studyPerson = new StudyPerson(
								person: person,
								role: role
							)
							studyPerson.save(flush: true)
						}

						flow.study.addToPersons(studyPerson)
					} else {
						println('.person ' + ids.person + ' or Role ' + ids.role + ' not found in database.')
					}
				}
			}
		} else {
			println('.no persons selected.')
			flow.study.persons.clear()
		}

	}

	/**
	 * re-usable code for handling subject form data in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleSubjects(flow, flash, params) {
		def names = [:]
		def errors = false
		def id = 0

		// iterate through subject templates
		flow.subjectTemplates.each() { subjectTemplate ->
			// iterate through subjects
			subjectTemplate.value.subjects.each() { subjectIncrement, subjectId ->
				// iterate through fields (= template fields and domain properties)
				flow.subjects[ subjectId ].giveFields().each() { subjectField ->
					// set the field
					flow.subjects[ subjectId ].setFieldValue(
						subjectField.name,
						params.get( 'subject_' + subjectId + '_' + subjectField.escapedName() )
					)
				}

				// validate subject
				if (!flow.subjects[ subjectId ].validate()) {
					errors = true
					this.appendErrors(flow.subjects[ subjectId ], flash.errors, 'subject_' + subjectId + '_')
				}
			}
		}

		return !errors
	}

	/**
	 * re-usable code for handling event form data in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleEvents(flow, flash, params) {
		def errors = false
		def template = null

		// handle the type of event
		if (params.eventType == 'event') {
			flow.event = new Event()
			template = params.remove('eventTemplate')
		} else if (params.eventType == 'sample') {
			flow.event = new SamplingEvent()
			template = params.remove('sampleTemplate')
		}

		// if a template is selected, get template instance
		if (template instanceof String && template.size() > 0) {
			params.template = Template.findByName(template)
		} else if (template instanceof Template) {
			params.template = template
		} else {
			params.template = null
		}

		// set template
		if (params.template) flow.event.template = params.template

		// update event instance with parameters
		flow.event.giveFields().each() { eventField ->
			flow.event.setFieldValue(eventField.name, params[ eventField.escapedName() ])	
		}

		// handle event objects
		flow.eventTemplates.each() { eventTemplate ->
			// iterate through events
			eventTemplate.getValue().events.each() { eventId ->
				// iterate through template fields
				flow.events[ eventId ].giveFields().each() { eventField ->
					if ( params.containsKey( 'event_' + eventId + '_' + eventField.escapedName() ) ) {
						flow.events[ eventId ].setFieldValue(eventField.name, params.get( 'event_' + eventId + '_' + eventField.escapedName() ) )
					}
				}

				// validate event
				if (!flow.events[ eventId ].validate()) {
					errors = true
					this.appendErrors(flow.events[ eventId ], flash.errors, 'event_' + eventId + '_')
				}
			}
		}

		// handle event grouping
		handleEventGrouping(flow, flash, params)

		return !errors
	}

	/**
	 * re-usable code for handling event grouping in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleEventGrouping(flow, flash, params) {
		// walk through eventGroups
		def g = 0
    		flow.eventGroups.each() { eventGroup ->
			def e = 0

			// reset events
			eventGroup.events = new HashSet()

			// iterate through events
			flow.events.each() {
				if (params.get('event_' + e + '_group_' + g) == 'on') {
					// add event to eventgroup
					if (it.value instanceof SamplingEvent) {
						eventGroup.addToSamplingEvents(it.value)
					} else {
						eventGroup.addToEvents(it.value)
					}
				}
				e++
			}
			g++
		}
	}

	/**
	 * re-usable code for handling subject grouping in a web flow
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleSubjectGrouping(flow, flash, params) {
		// iterate through event groups
		def g = 0
		flow.eventGroups.each() { eventGroup ->
			// reset subjects
			eventGroup.subjects = new HashSet()

			// iterate through subjects
			flow.subjects.each() { subjectId, subject ->
				// is this combination set?
				if (params.get('subject_' + subjectId + '_group_' + g) != null) {
					eventGroup.addToSubjects(subject)
				}
			}

			g++
		}
	}


	/**
	 * re-usable code for handling samples
	 * @param Map LocalAttributeMap (the flow scope)
	 * @param Map localAttributeMap (the flash scope)
	 * @param Map GrailsParameterMap (the flow parameters = form data)
	 * @return boolean
	 */
	def handleSamples(flow, flash, params) {
		flash.errors = [:]
		def errors = false		
		def id = 0

		// iterate through samples
		flow.samples.each() { sampleData ->
			def sample = sampleData.sample
			def sampleTemplateName = params.get('template_'+id)
			def oldSampleTemplateName = sampleData.sample.template.toString()

			// has the sample template for this sample changed
			if (sampleTemplateName && sampleTemplateName.size() > 0 && oldSampleTemplateName != sampleTemplateName) {
				// yes, has the template changed?
				println ".changing template for sample ${id} to ${sampleTemplateName}"

				// decrease previous template count
				if (oldSampleTemplateName && flow.sampleTemplates[ oldSampleTemplateName ]) {
					flow.sampleTemplates[ oldSampleTemplateName ].count--

					if (flow.sampleTemplates[ oldSampleTemplateName ].count < 1) {
						// no samples left, remove template altogether
						flow.sampleTemplates.remove( oldSampleTemplateName )
					}
				} else {
					// increate main template counter?
					flow.samplesWithTemplate++
				}

				// increase current template count
				if (!flow.sampleTemplates[ sampleTemplateName ]) {
					flow.sampleTemplates[ sampleTemplateName ] = [
						name		: sampleTemplateName,
						template	: Template.findByName( sampleTemplateName ),
						count		: 1
					]
				} else {
					// increase count
					flow.sampleTemplates[ sampleTemplateName ].count++
				}

				// change template
				sampleData.sample.template = flow.sampleTemplates[ sampleTemplateName ].template
			}

			// handle values
			sampleData.sample.giveFields().each() { sampleField ->
				if ( params.containsKey( 'sample_'+id+'_'+sampleField.escapedName() ) ) {
					sampleData.sample.setFieldValue( sampleField.name, params.get( 'sample_'+id+'_'+sampleField.escapedName() ) )
				}
			}

			// validate sample
			if (!sampleData.sample.validate()) {
				errors = true
				this.appendErrors(sampleData.sample, flash.errors, 'sample_' + id + '_' )
			}

			// increase counter
			id++
		}

		return !errors
	}

	/**
	 * groovy / java equivalent of php's ucwords function
	 *
	 * Capitalize all first letters of seperate words
	 *
	 * @param String
	 * @return String
	 */
	def ucwords(String text) {
		def newText = ''

		// change case to lowercase
		text = text.toLowerCase()

		// iterate through words
		text.split(" ").each() {
			newText += it[0].toUpperCase() + it.substring(1) + " "
		}

		return newText.substring(0, newText.size()-1)
	}

	/**
	 * return the object from a map of objects by searching for a name
	 * @param String name
	 * @param Map map of objects
	 * @return Object
	 */
	def getObjectByName(name, map) {
		def result = null
		map.each() {
			if (it.name == name) {
				result = it
			}
		}

		return result
	}

	/**
	 * transform domain class validation errors into a human readable
	 * linked hash map
	 * @param object validated domain class
	 * @return object  linkedHashMap
	 */
	def getHumanReadableErrors(object) {
		def errors = [:]
		object.errors.getAllErrors().each() {
			def message = it.toString()

			//errors[it.getArguments()[0]] = it.getDefaultMessage()
			errors[it.getArguments()[0]] = message.substring(0, message.indexOf(';'))
		}

		return errors
	}

	/**
	 * append errors of a particular object to a map
	 * @param object
	 * @param map linkedHashMap
	 * @void
	 */
	def appendErrors(object, map) {
		this.appendErrorMap(this.getHumanReadableErrors(object), map)
	}

	def appendErrors(object, map, prepend) {
		this.appendErrorMap(this.getHumanReadableErrors(object), map, prepend)
	}

	/**
	 * append errors of one map to another map
	 * @param map linkedHashMap
	 * @param map linkedHashMap
	 * @void
	 */
	def appendErrorMap(map, mapToExtend) {
		map.each() {key, value ->
			mapToExtend[key] = ['key': key, 'value': value, 'dynamic': false]
		}
	}

	def appendErrorMap(map, mapToExtend, prepend) {
		map.each() {key, value ->
			mapToExtend[prepend + key] = ['key': key, 'value': value, 'dynamic': true]
		}
	}

	/**
	 * Parses a RelTime string and returns a nice human readable string
	 *
	 * @return Human Readable string or a HTTP response code 400 on error
	 */
	def ajaxParseRelTime = {
		if (params.reltime == null) {
			response.status = 400
			render('reltime parameter is expected')
		}

		try {
			def reltime = RelTime.parseRelTime(params.reltime)
			render reltime.toPrettyString()
		} catch (IllegalArgumentException e) {
			response.status = 400
			render(e.getMessage())
		}
	}

	/**
	 * Proxy for searching PubMed articles (or other articles from the Entrez DB).
	 *
	 * This proxy is needed because it is not allowed to fetch XML directly from a different
	 * domain using javascript. So we have the javascript call a function on our own domain
	 * and the proxy will fetch the data from Entrez
	 *
	 * @since	20100609
	 * @param	_utility	The name of the utility, without the complete path. Example: 'esearch.fcgi'
	 * @return	XML
	 */
	def entrezProxy = {
		// Remove unnecessary parameters
		params.remove( "action" )
		params.remove( "controller" )

		def url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
		def util = params.remove( "_utility" )
		def paramString = params.collect { k, v -> k + '=' + v.encodeAsURL() }.join( '&' );

		def fullUrl = url + '/' + util + '?' + paramString;

		// Return the output of the request
		// render fullUrl;
		render(
                    text:           new URL( fullUrl ).getText(),
                    contentType:    "text/xml",
                    encoding:       "UTF-8"
                );
	}
}