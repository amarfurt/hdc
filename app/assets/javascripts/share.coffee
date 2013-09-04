class Records extends Backbone.View
	initialize: ->
		@editMode = false
	toggleEditMode: ->
		@editMode = not @editMode
		if @editMode
			$(recordFieldset).removeAttr("disabled")
			$(shareButton).removeAttr("disabled")
		else
			$(recordFieldset).attr("disabled", "disabled")
			$(shareButton).attr("disabled", "disabled")

class Circles extends Backbone.View
	initialize: ->
		@circleViews = _.map($(".circle", @el), 
			(circle) -> new Circle el: $ circle)
	getShared: ->
		circleIds = []
		$(".circle:checked", @el).each (i, checked) ->
			circleIds.push $(checked).attr("name")
		jsRoutes.controllers.Share.sharedRecords(circleIds).ajax
			context: this
			success: (data) ->
				$(records).replaceWith(data)
			error: (err) ->
				console.log("Error: " + err)
	toggleEditMode: ->
		_.each(@circleViews, (view) -> view.toggleEditMode())

class Circle extends Circles
	initialize: ->
		@id = @el.attr("name")
		@editMode = false
	events:
		"click": "selectionChanged"
	selectionChanged: (e) ->
		if not @editMode
			Circle.__super__.getShared()
	toggleEditMode: ->
		@editMode = not @editMode

class Share extends Backbone.View
	initialize: ->
		@circles = new Circles el: $("#circles", @el)
		@records = new Records el: $("#records", @el)
	events:
		"click #editButton": "toggleEditMode"
	toggleEditMode: (e) ->
		@circles.toggleEditMode()
		@records.toggleEditMode()
		
# Instantiate views
$ ->
	share = new Share el: $("#share")