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

$.fn.exists = () ->
    this.length isnt 0

# String functions
String.prototype.startsWith = (prefix) ->
    this.indexOf(prefix) is 0

String.prototype.endsWith = (suffix) ->
    `this.match(suffix+"$") == suffix`

String.prototype.splitSearch = (term) ->
	term.split(/[ ,\\+]+/)