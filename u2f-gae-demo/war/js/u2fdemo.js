

function getTokens(callback) {
  $.post('/GetTokens', {}, function(tokens) {
      callback(tokens);
  },
  'json');
}

function initialFetchTokens() {
	getTokens(function(tokens) {
		for (var index in tokens) {
			addOneToken(tokens[index]);
		}
	});
}

function addOneToken(token) {
	console.log(token);
	document
		.getElementById('tokens')
		.appendChild(tokenToDom(token));
	
	// now that we've added the card into the dom, let's bind the mouseover
	// events to it:
	$("#" + token.public_key)
	  .mouseover(function() { 
		  $(this).find(".buttonBar").addClass("visible"); 
	    })
	  .mouseout(function() { 
		  $(this).find(".buttonBar").removeClass("visible"); 
		});
}

function removeOneToken(publicKey) {
	console.log(publicKey);
	$(document.getElementById(publicKey)).remove();
}

function tokenToDom(token) {
	var timeString = new Date(token.enrollment_time).toLocaleString();
	
	var template = document.getElementById('cardTemplate');
	var card = template.content.cloneNode(true);
	card.querySelector('.card').setAttribute("id", token.public_key);
	card.querySelector('.issuer').textContent = token.issuer;			
	card.querySelector('.enrollmentTimeValue').textContent = timeString;			
	card.querySelector('.keyHandle').textContent = token.key_handle;			
	card.querySelector('.publicKey').textContent = token.public_key;	
	
	$(card.querySelector('.removeCardButton'))
	  .button()
	  .click(function() { onRemoveToken(token.public_key); });
	
	return card;
}

function onAddToken() {
  $.post('/BeginEnroll', {
        'reregistration' : document.querySelector('#reregistration').checked
      }, null, 'json')
   .done(function(beginEnrollResponse) {
   	  console.log(beginEnrollResponse);
      enrollHandler.handleRegistrationRequest(
        [ beginEnrollResponse.enroll_data ], 
        beginEnrollResponse.sign_data);
    });
}

function onRemoveToken(publicKey) {
  $.post('/RemoveToken', {
      'public_key' : publicKey
  	}, null, 'json')
  	.done(function(e) {
  	  removeOneToken(publicKey);
  	})
  	.fail(function(xhr, status) {
  	  showError("couldn't remove token: " + status);
  	});
}

function onEnrollSuccess(finishEnrollData) {
  hideMessage();
  console.log(finishEnrollData);
  $.post('/FinishEnroll', finishEnrollData, null, 'json')
    .done(addOneToken)
    .fail(function(xhr, status) { showError(status); })
}

function onGnubbyEnrollResponse() {
  $.post('/AddToken', {}, function(obj) {
      addOneToken(obj);
    },
    'json');
}

function onTestAuthentication() {
  $.post('/BeginSign', {}, null, 'json')
   .done(function(signData) {
      console.log(signData);
      showMessage("please touch the token");
      signHandler.handleAuthenticationRequest(signData);
   }) 
   .fail(function() {
		showError("can't authenticate");
   });
}

function onSignSuccess(responseData) {
	console.log(responseData);
    hideMessage();
    $.post('/FinishSign', responseData, null, 'json')
      .done(processFinishSignResult)
      .fail(function(xhr, status) {
        showError(status);
      });
}

function processFinishSignResult(response) {
	console.log(response);
	var cardContent = $(document
	    .getElementById(response.public_key)
	    .querySelector(".cardContent")); 
	cardContent.addClass("highlight");
	window.setTimeout(function() { cardContent.removeClass("highlight", 2000); }, 500 );
}

function onEnrollError(code) {
  if (code == u2f.CryptoTokenCodeTypes.ALREADY_ENROLLED) {
	showError('device already enrolled');
  } else if (code == u2f.CryptoTokenCodeTypes.BROWSER_ERROR) {
	showError('chrome error');
  } else if (code == u2f.CryptoTokenCodeTypes.NO_EXTENSION) {
	showError('the extension is not installed');
  } else if (code == u2f.CryptoTokenCodeTypes.WAIT_TOUCH) {
	showMessage('please touch the token');
  } else if (code == u2f.CryptoTokenCodeTypes.NO_GNUBBIES) {
	showError('no U2F tokens found');
    // Don't do anything here
  } else if (code == u2f.CryptoTokenCodeTypes.LONG_WAIT) {
	showError('waiting on gnubby update');
  } else if (code == u2f.CryptoTokenCodeTypes.UNKNOWN_ERROR) {
	showError('unknown error code=' + code);
  } else {
	showError('other error');
  }
}

function onSignError(code) {
  console.log(code);
  if (code == u2f.CryptoTokenCodeTypes.NONE_PLUGGED_ENROLLED) {
	showError('U2F token is not registered');
  } else if (code == u2f.CryptoTokenCodeTypes.TOUCH_TIMEOUT) {
	showError('touch timeout');
  } else if (code == u2f.CryptoTokenCodeTypes.NO_DEVICES_ENROLLED) {
	showError('no devices enrolled');
  } else if (code == u2f.CryptoTokenCodeTypes.BROWSER_ERROR) {
	showError('browser error');
  } else if (code == u2f.CryptoTokenCodeTypes.NO_EXTENSION) {
    showError('no extension error');
  } else if (code == u2f.CryptoTokenCodeTypes.WAIT_TOUCH ||
      code == u2f.CryptoTokenCodeTypes.NO_GNUBBIES) {
	showMessage('please touch the token');
  } else {
    showError('unknown error code=' + code);
  }
}


function showError(message) {
   hideMessage();
   console.log(message);
   $('#errormessage').empty();
   $('#errormessage').text(message);
   $('#errorbox').addClass('visible').removeClass('invisible');
} 

function showMessage(message) {
   console.log(message);
   $('#infomessage').empty();
   $('#infomessage').text(message);
   $('#infobox').addClass('visible').removeClass('invisible');
} 

function hideMessage() {
   $('#infobox').addClass('invisible').removeClass('visible');
} 

function hideError() {
   $('#errorbox').addClass('invisible').removeClass('visible');
} 

var enrollHandler = new u2f.CryptoTokenHandler(onEnrollSuccess, onEnrollError);
var signHandler = new u2f.CryptoTokenHandler(onSignSuccess, onSignError);
