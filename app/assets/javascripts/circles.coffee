class Member extends Backbone.View
	initialize: ->
		@circleId = @el.attr("circle-id")
		@memberId = @el.attr("member-id")
	events:
		"click .removeMember": "removeMember"
	removeMember: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Circles.removeMember(@circleId, @memberId).ajax
			context: this
			success: (data) ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				console.error("Removing member failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".removeMember").hide()
		else
			@el.children(".removeMember").show()

class Circle extends Backbone.View
	initialize: ->
		@id = @el.attr("id")
		$(".member", @el).each (i, member) ->
			new Member el: $(member)
	events:
		"click .deleteCircle": "deleteCircle"
		"keyup .userSearch": "userSearch"
	deleteCircle: (e) ->
		@loading(true)
		jsRoutes.controllers.Circles.delete(@id).ajax
			context: this
			success: (response) ->
				@loading(false)
				window.location.replace(response)
			error: (err) ->
				@loading(false)
				console.error("Deleting circle failed.")
				console.error(err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".addUser").hide()
			@el.children(".deleteCircle").hide()
		else
			@el.children(".addUser").show()
			@el.children(".deleteCircle").show()
	userSearch: (e) ->
		search = $(".userSearch", @el).val()
		if search.length >= 3
			jsRoutes.controllers.Circles.searchUsers(@id, search).ajax
				context: this
				success: (data) ->
					$(".searchResults", @el).replaceWith(data)
				error: (err) ->
					console.error("User search failed.")
					console.error(err.responseText)

class CircleTab extends Backbone.View
	initialize: ->
		@id = @el.attr("tab-id")
		@name = $(".circleName", @el).editInPlace
			context: this
			onChange: @renameCircle
	renameCircle: (name) ->
		jsRoutes.controllers.Circles.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@name.editInPlace("close", data)
			error: (err) ->
				console.error("Renaming circle failed.")
				console.error(err.responseText)

# Instantiate views
$ ->
	_.each $(".circleTab"), (circleTab) -> new CircleTab el: $ circleTab
	_.each $(".circle"), (circle) -> new Circle el: $ circle
