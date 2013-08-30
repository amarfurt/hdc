class CircleMember extends Backbone.View
	initialize: ->
		@circle_id = @el.attr("circle-id")
		@id = @el.attr("id")
	events:
		"click .removeMember": "removeMember"
	removeMember: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Circles.removeMember(@circle_id).ajax
			context: this
			data:
				name: @id
			success: (data) ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				alert err.responseText
				$.error("Error: " + err.responseText)
	loading: (display) ->
		if (display)
			@el.children(".removeMember").hide()
		else
			@el.children(".removeMember").show()

class Circle extends Backbone.View
	initialize: ->
		@id = @el.attr("circle-id")
		@name = $(".circleName", @el).editInPlace
			context: this
			onChange: @renameCircle
		$(".member", @el).each (i, member) ->
				new CircleMember el: $(member)
	renameCircle: (name) ->
		@loading(true)
		jsRoutes.controllers.Circles.rename(@id).ajax
			context: this
			data:
				name: name
			success: (data) ->
				@loading(false)
				@name.editInPlace("close", data)
			error: (err) ->
				@loading(false)
				alert err.responseText
				$.error("Error: " + err)
	events:
		"click .deleteCircle": "deleteCircle"
		"click .addMember":	"addMember"
	deleteCircle: (e) ->
		e.preventDefault()
		@loading(true)
		jsRoutes.controllers.Circles.delete(@id).ajax
			context: this
			success: ->
				@el.remove()
				@loading(false)
			error: (err) ->
				@loading(false)
				$.error("Error: " + err)
	loading: (display) ->
		if (display)
			@el.children(".addMember").hide()
			@el.children(".deleteCircle").hide()
		else
			@el.children(".addMember").show()
			@el.children(".deleteCircle").show()
	addMember: (e) ->
		e.preventDefault()
		@loadingMember(true)
		@member = $(".newMember", @el).editInPlace
			context: this
			onChange: @addNewMember
		$(".newMember", @el).editInPlace("edit")
	addNewMember: (member) ->
		jsRoutes.controllers.Circles.addMember(@id).ajax
			context: this
			data:
				name: member
			success: (data) ->
				@member.editInPlace("close", "New member")
				@loadingMember(false)
				_view = new CircleMember
					el: $(data).appendTo("#"+@id+".list-group")
				$("#"+@id+".noMembers", @el).addClass("hidden")
			error: (err) ->
				@member.editInPlace("close", "New member")
				@loadingMember(false)
				alert err.responseText
				$.error("Error: " + err.responseText)
	loadingMember: (display) ->
		if (display)
			$(".addMember", @el).hide()
			$(".newMember", @el).show()
		else
			$(".newMember", @el).hide()
			$(".addMember", @el).show()

class CirclesManager extends Backbone.View
	initialize: ->
		@el.children(".circle").each (i, circle) ->
			new Circle el: $(circle)
		$(".addCircle").click @addCircle
	addCircle: (e) ->
		jsRoutes.controllers.Circles.add().ajax
			context: this
			success: (data) ->
				_view = new Circle
					el: $(data).appendTo("#circles")
				_view.el.find(".circleName").editInPlace("edit")
			error: (err) ->
				$.error("Error: " + err)

# Instantiate views
$ ->
	mngr = new CirclesManager el: $("#circles")
