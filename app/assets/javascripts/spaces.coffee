class Space extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
		
		jsRoutes.controllers.Spaces.getVisualizationURL(@id).ajax
			context: this
			success: (url) ->
				@url = url
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
		loadFilters(@url, @records, @id, compare)
		loadSpace(@url, @records, @id, compare)
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
	
	url = jsRoutes.controllers.visualizations.RecordList.load().url
	jsRoutes.controllers.Spaces.loadAllRecords().ajax
		context: this
		success: (data) ->
			window.records = data
			
			# Load the filters
			loadFilters(url, window.records, spaceId, false)
			
			# Load the space
			loadSpace(url, window.records, spaceId, false)
			
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
loadSpace = (url, records, spaceId, compare) ->
	iframeName = if not compare then "iframe" else "iframe2"
	json = {"spaceId": spaceId, "records": records}
	$.ajax
		type: "POST"
		url: url
		context: this
		contentType: "application/json; charset=utf-8"
		data: JSON.stringify(json)
		success: (response) ->
			iframeId = iframeName+"-"+spaceId
			$("#"+iframeId).replaceWith('<iframe id="'+iframeId+'" src="'+response+'" height="420px" width="100%"></iframe>')

filterRecords = (url, records, spaceId, compare) ->
	creatorFilter = if not compare then "filterCreator" else "filterCreator2"
	ownerFilter = if not compare then "filterOwner" else "filterOwner2"
	sliderFilter = if not compare then "filterSlider" else "filterSlider2"
	
	creator = $("#"+creatorFilter+"-"+spaceId).attr("value")
	owner = $("#"+ownerFilter+"-"+spaceId).attr("value")
	range = $("#"+sliderFilter+"-"+spaceId).attr("value")	
	day = 1000 * 60 * 60 *24
	startdate = formatDate(new Date(Number(range.substr(0, range.indexOf(",")))))	
	enddate = formatDate(new Date(day+Number(range.substr(range.indexOf(",")+1))))
	$("#"+sliderFilter+"Range-"+spaceId).text(startdate + " to " + enddate) 
	
	records = filterByProperty(records, "creator", creator)
	records = filterByProperty(records, "owner", owner)
	records = filterByPropertyRange(records, "created", startdate, enddate)
	loadSpace(url, records, spaceId, compare)

filterByProperty = (list, property, value) ->
	return _.filter list, (record) -> if value is "any" then true else record[property] is value

filterByPropertyRange = (list, property, minvalue, maxvalue) ->
	return _.filter list, (record) -> console.log(record[property]);return (record[property] >= minvalue && record[property] <= maxvalue)

formatDate = (date) -> 
     return (date.getFullYear()+"-"+(if date.getMonth()<9 then "0" else "")+(1+date.getMonth())+"-"+(if date.getDate()<10 then "0" else "")+date.getDate()) 

loadFilters = (url, records, spaceId, compare) ->
	creators = []
	owners = []
	_.each records, (record) ->
		creators.push record.creator
		owners.push record.owner
	# Load the names (synchronously; needed afterwards)
	ids = _.union(creators, owners)
	idsToNames = {}
	_.each ids, (id) ->
		jsRoutes.controllers.Users.getName(id).ajax
			async: false
			success: (name) ->
				idsToNames[id] = name
			error: (err) ->
				console.error("Error when retrieving a user's name.")
				console.error(err.responseText)
	
	# Define the names
	creatorFilter = if not compare then "filterCreator" else "filterCreator2"
	ownerFilter = if not compare then "filterOwner" else "filterOwner2"
	sliderFilter = if not compare then "filterSlider" else "filterSlider2"
	
	# Add filter options to select
	$("#"+creatorFilter+"-"+spaceId).empty()
	$("#"+ownerFilter+"-"+spaceId).empty()
	$("#"+creatorFilter+"-"+spaceId).append('<option value="any">anyone</option>')
	$("#"+ownerFilter+"-"+spaceId).append('<option value="any">anyone</option>')
	_.each (_.uniq creators), (creator) -> $("#"+creatorFilter+"-"+spaceId).append('<option value="' + creator + '">' + idsToNames[creator] + '</option>')
	_.each (_.uniq owners), (owner) -> $("#"+ownerFilter+"-"+spaceId).append('<option value="' + owner + '">' + idsToNames[owner] + '</option>')
	
	# Date filter slider
	day = 1000 * 60 * 60 *24
	lastdate = new Date()
	firstdate = new Date(lastdate - day * 365 * 5) 	
	$("#"+sliderFilter+"-"+spaceId).slider({ min:firstdate.getTime(), max:lastdate.getTime(), step:day, value:[firstdate.getTime(), lastdate.getTime()], formater:(a) -> return formatDate(new Date(a))  })
	
		
	# Register the filter events
	$("#"+creatorFilter+"-"+spaceId).on "change", (e) ->
		filterRecords(url, records, spaceId, compare)
	$("#"+ownerFilter+"-"+spaceId).on "change", (e) ->
		filterRecords(url, records, spaceId, compare)
	$("#"+sliderFilter+"-"+spaceId).on "slideStop", (e) -> 
	    filterRecords(url, records, spaceId, compare)	
