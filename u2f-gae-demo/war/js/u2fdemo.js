
///////////////////////////////////////////////////////////////////////////////
// UI Functions                                                              //
///////////////////////////////////////////////////////////////////////////////

function addTokenInfoToPage(token) {
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
    .click(function() { 
      sendRemoveTokenRequest(token.public_key); 
     });

  return card;
}

function removeTokenInfoFromPage(publicKey) {
  console.log(publicKey);
  $("#" + publicKey).remove();
}

function showError(message) {
  hideMessage();
  console.log(message);
  $('#errormessage').empty();
  $('#errormessage').text(message);
  $('#errorbox').addClass('visible').removeClass('invisible');
} 

function hideError() {
  $('#errorbox').addClass('invisible').removeClass('visible');
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

function highlightTokenCardOnPage(token) {
  console.log(token);
  var cardContent = $("#" + token.public_key).find(".cardContent");
  
  cardContent.addClass("highlight");
  window.setTimeout(function() { cardContent.removeClass("highlight", 2000); }, 500 );
}


///////////////////////////////////////////////////////////////////////////////
// AJAX Calls                                                                //
///////////////////////////////////////////////////////////////////////////////

function fetchAllUserTokens(callback) {
  $.post('/GetTokens', {}, null, 'json')
   .done(function(tokens) {
     for (var index in tokens) {
       addTokenInfoToPage(tokens[index]);
     }
   });
}

function sendRemoveTokenRequest(publicKey) {
  $.post('/RemoveToken', {
      'public_key' : publicKey
    }, null, 'json')
    .done(function(e) {
      removeTokenInfoFromPage(publicKey);
    })
    .fail(function(xhr, status) {
      showError("couldn't remove token: " + status);
    });
}

function sendBeginEnrollRequest() {
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

function sendBeginSignRequest() {
  $.post('/BeginSign', {}, null, 'json')
   .done(function(signData) {
      console.log(signData);
      showMessage("please touch the token");
      signHandler.handleAuthenticationRequest(signData);
   }) 
   .fail(function(xhr, status) {
      showError("can't authenticate: " + status);
   });
}

///////////////////////////////////////////////////////////////////////////////
// U2F Token Callbacks                                                       //
///////////////////////////////////////////////////////////////////////////////

function onTokenEnrollSuccess(finishEnrollData) {
  hideMessage();
  console.log(finishEnrollData);
  $.post('/FinishEnroll', finishEnrollData, null, 'json')
   .done(addTokenInfoToPage)
   .fail(function(xhr, status) { 
      showError(status); 
   })
}

function onTokenSignSuccess(responseData) {
	console.log(responseData);
    hideMessage();
    $.post('/FinishSign', responseData, null, 'json')
      .done(highlightTokenCardOnPage)
      .fail(function(xhr, status) {
        showError(status);
      });
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
	showError('other error code=' + code);
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
    showError('the extension is not installed');
  } else if (code == u2f.CryptoTokenCodeTypes.WAIT_TOUCH ||
    code == u2f.CryptoTokenCodeTypes.NO_GNUBBIES) {
	showMessage('please touch the token');
  } else {
    showError('unknown error code=' + code);
  }
}

var enrollHandler = new u2f.CryptoTokenHandler(onTokenEnrollSuccess, onEnrollError);
var signHandler = new u2f.CryptoTokenHandler(onTokenSignSuccess, onSignError);
