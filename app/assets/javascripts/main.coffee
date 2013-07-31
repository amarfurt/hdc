$.fn.editInPlace = (method, options...) ->
    this.each ->
        methods = 
            # public methods
            init: (options) ->
                valid = (e) =>
                    newValue = @input.val()
                    options.onChange.call(options.context, newValue)
                cancel = (e) =>
                    @el.show()
                    @input.hide()
                @el = $(this).dblclick(methods.edit)
                @input = $("<input type='text' />")
                    .insertBefore(@el)
                    .keyup (e) ->
                        switch(e.keyCode)
                            # Enter key
                            when 13 then $(this).blur()
                            # Escape key
                            when 27 then cancel(e)
                    .blur(valid)
                    .hide()
            edit: ->
                @input
                    .val(@el.text())
                    .show()
                    .focus()
                    .select()
                @el.hide()
            close: (newName) ->
                @el.text(newName).show()
                @input.hide()
        # jQuery approach: http://docs.jquery.com/Plugins/Authoring
        if ( methods[method] )
            return methods[ method ].apply(this, options)
        else if (typeof method == 'object')
            return methods.init.call(this, method)
        else
            $.error("Method " + method + " does not exist.")
            
class Circle extends Backbone.View
	initialize: ->
        @loadingMember(false)
        @id = @el.attr("circle-id")
        @name = $(".circleName", @el).editInPlace
            context: this
            onChange: @renameCircle
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
                $.error("Error: " + err)
	events:
        "click .deleteCircle": "deleteCircle"
        "click .addMember": "addMember"
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
            @el.children(".loader").show()
        else
            @el.children(".addMember").show()
            @el.children(".deleteCircle").show()
            @el.children(".loader").hide()
    addMember: (e) ->
    	e.preventDefault()
    	@loadingMember(true)
    	@member = $(".newMember", @el).editInPlace
            context: this
            onChange: @addNewMember
    	@el.children(".newMember").editInPlace("edit")
    addNewMember: (member) ->
    	jsRoutes.controllers.Circles.addMember(@id).ajax
    		context: this
    		data:
    			name: member
    		success: (data) ->
    			@member.editInPlace("close", data)
    			@loadingMember(false)
    		error: (err) ->
    			@member.editInPlace("close", "New member")
    			@loadingMember(false)
    			alert err.responseText
    			$.error("Error: " + err.responseText)
    loadingMember: (display) ->
    	if (display)
            @el.children(".addMember").hide()
            @el.children(".newMember").show()
        else
            @el.children(".newMember").hide()
            @el.children(".addMember").show()

class CircleManager extends Backbone.View
	initialize: ->
		@el.children("li").each (i, circle) ->
			new Circle
				el: $(circle)
		$("#addCircle").click @addCircle
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
	mngr = new CircleManager el: $("#circles")