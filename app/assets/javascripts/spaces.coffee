class Space extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
		if @el.hasClass("active")
			@loadSpaceRecords()
	events:
		"click .showCompare": "showCompare"
		"click .hideCompare": "hideCompare"
		"click .deleteSpace": "deleteSpace"
		"keyup .recordSearch": "recordSearch"
	showCompare: (e) ->
		e.preventDefault()
		$(".showCompare", @el).addClass("hidden")
		$(".hideCompare", @el).removeClass("hidden")
		$(".compare1", @el).addClass("col-lg-6")
		$(".compare2", @el).removeClass("hidden")
		@loadSpace(true)
	hideCompare: (e) ->
		e.preventDefault()
		$(".hideCompare", @el).addClass("hidden")
		$(".showCompare", @el).removeClass("hidden")
		$(".compare1", @el).removeClass("col-lg-6")
		$(".compare2", @el).addClass("hidden")
	loadSpaceRecords: ->
		jsRoutes.controllers.Spaces.loadRecords(@id).ajax
			context: this
			success: (data) ->
				@records = _.filter window.records, (record) -> record._id in data
				@loadSpace(false)
			error: (err) ->
				console.error("Error when loading spaces.")
				console.error(err.responseText)
	loadSpace: (compare) ->
		loadFilters(@records, @id, compare)
		postForm(@records, @id, compare)
	deleteSpace: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Spaces.delete(@id).ajax
			context: this
			success: (response) ->
				@loading(false)
				window.location.replace(response)
			error: (err) ->
				@loading(false)
				console.error("Deleting space failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".addRecord").hide()
			@el.children(".deleteSpace").hide()
		else
			@el.children(".addRecord").show()
			@el.children(".deleteSpace").show()
	recordSearch: (e) ->
		search = $(".recordSearch", @el).val()
		jsRoutes.controllers.Spaces.searchRecords(@id, search).ajax
			context: this
			success: (data) ->
				$(".searchResults", @el).replaceWith(data)
			error: (err) ->
				console.error("Record search failed.")
				console.error(err.responseText)

class SpaceTab extends Backbone.View
	initialize: ->
		@id = @el.attr("tab-id")
		@name = $(".spaceName", @el).editInPlace
			context: this
			onChange: @renameSpace
	events:
		"click": "loadSpace"
	renameSpace: (name) ->
		jsRoutes.controllers.Spaces.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@name.editInPlace("close", data)
			error: (err) ->
				console.error("Renaming space failed.")
				console.error(err.responseText)
	loadSpace: ->
		@content.loadSpaceRecords()

# Instantiate views
$ ->
	# Load all records and default space
	window.records = []
	spaceId = "default"
	jsRoutes.controllers.Spaces.loadAllRecords().ajax
		context: this
		success: (data) ->
			window.records = data
			
			# Load the filters
			loadFilters(window.records, spaceId, false)
			
			# Load the space
			postForm(window.records, spaceId, false)
			
			###
			json = JSON.stringify({"spaceId": null, "records": data})
			jsRoutes.controllers.Visualizations.jsonList().ajax
				context: this
				type: "POST"
				contentType: "application/json; charset=utf-8"
				data: json
				success: (response) ->
					console.log(response)
					#$("#space-default").html(response)
					$("#iframe-default").contents().find("html").html(response)
				error: (err) ->
					console.error("Error when loading visualization")
					console.error(err.responseText)
			###
			
			# Load the other spaces (window.records needs to be set)
			tabs = _.map $(".spaceTab"), (spaceTab) -> new SpaceTab el: $ spaceTab
			spaces = _.map $(".space"), (space) -> new Space el: $ space
			_.each tabs, (tab) ->
				_.each spaces, (space) ->
					if tab.id is space.id then tab.content = space
			
		error: (err) ->
			console.error("Error when loading records.")
			console.error(err.responseText)

# General functions
postForm = (records, spaceId, compare) ->
	formName = if not compare then "form" else "form2"
	$("#"+formName+"-"+spaceId).empty()
	$("#"+formName+"-"+spaceId).append('<input type="hidden" name="spaceId" value="' + spaceId + '">')
	_.each records, (record) ->
		$("#"+formName+"-"+spaceId).append('<input type="hidden" name="' + record._id + ' creator" value="' + record.creator + '">')
		$("#"+formName+"-"+spaceId).append('<input type="hidden" name="' + record._id + ' owner" value="' + record.owner + '">')
		$("#"+formName+"-"+spaceId).append('<input type="hidden" name="' + record._id + ' created" value="' + record.created + '">')
		$("#"+formName+"-"+spaceId).append('<input type="hidden" name="' + record._id + ' data" value="' + record.data + '">')
	$("#"+formName+"-"+spaceId).submit()
	
filterRecords = (records, spaceId, compare) ->
	creatorFilter = if not compare then "filterCreator" else "filterCreator2"
	ownerFilter = if not compare then "filterOwner" else "filterOwner2"
	creator = $("#"+creatorFilter+"-"+spaceId).attr("value")
	owner = $("#"+ownerFilter+"-"+spaceId).attr("value")
	records = filterByProperty(records, "creator", creator)
	records = filterByProperty(records, "owner", owner)
	postForm(records, spaceId, compare)

filterByProperty = (list, property, value) ->
	return _.filter list, (record) -> if value is "any" then true else record[property] is value

loadFilters = (records, spaceId, compare) ->
	creators = []
	owners = []
	_.each records, (record) ->
		creators.push record.creator
		owners.push record.owner
	# Load the names (synchronously; needed afterwards)
	ids = _.union(creators, owners)
	idsToNames = {}
	_.each ids, (id) ->
		jsRoutes.controllers.api.UserInfo.getName(id).ajax
			async: false
			success: (name) ->
				idsToNames[id] = name
			error: (err) ->
				console.error("Error when retrieving a user's name.")
				console.error(err.responseText)
	
	# Define the names
	creatorFilter = if not compare then "filterCreator" else "filterCreator2"
	ownerFilter = if not compare then "filterOwner" else "filterOwner2"
	
	# Add filter options to select
	$("#"+creatorFilter+"-"+spaceId).empty()
	$("#"+ownerFilter+"-"+spaceId).empty()
	$("#"+creatorFilter+"-"+spaceId).append('<option value="any">anyone</option>')
	$("#"+ownerFilter+"-"+spaceId).append('<option value="any">anyone</option>')
	_.each (_.uniq creators), (creator) -> $("#"+creatorFilter+"-"+spaceId).append('<option value="' + creator + '">' + idsToNames[creator] + '</option>')
	_.each (_.uniq owners), (owner) -> $("#"+ownerFilter+"-"+spaceId).append('<option value="' + owner + '">' + idsToNames[owner] + '</option>')
	
	# Register the filter events
	$("#"+creatorFilter+"-"+spaceId).on "change", (e) ->
		filterRecords(records, spaceId, compare)
	$("#"+ownerFilter+"-"+spaceId).on "change", (e) ->
		filterRecords(records, spaceId, compare)
