class Records extends Backbone.View

class Circles extends Backbone.View
	initialize: ->
		$(".circle", @el).each (i, circle) ->
			new Circle el: $(circle)
	getShared: (e) ->
		circleIds = []
		$(".circle:checked", @el).each (i, checked) ->
			circleIds.push $(checked).attr("name")
		jsRoutes.controllers.Share.sharedRecords(circleIds).ajax
			context: this
			success: (data) ->
				$(records).replaceWith(data)
			error: (err) ->
				console.log("Error.")
				console.log(err)

class Circle extends Circles
	initialize: ->
		@id = @el.attr("name")
	events:
		"click": "selectionChanged"
	selectionChanged: (e) ->
		Circle.__super__.getShared(e)

class Share extends Backbone.View
	initialize: ->
		new Circles el: $("#circles", @el)
		new Records el: $("#records", @el)
		

# Instantiate views
$ ->
	share = new Share el: $("#share")