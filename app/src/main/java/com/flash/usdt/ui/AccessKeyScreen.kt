package com.flash.usdt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flash.usdt.R
import com.flash.usdt.ui.theme.FlashUsdtTheme
import kotlinx.coroutines.launch

private const val USDT_LOGO_URL =
  "https://upload.wikimedia.org/wikipedia/commons/0/01/USDT_Logo.png"

@Composable
fun AccessKeyScreen(viewModel: AccessKeyViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current

  val helpMessage = stringResource(R.string.help_access_key_message)
  val supportUrl = stringResource(R.string.support_telegram_url)
  val unableToOpenLinkMessage = stringResource(R.string.unable_to_open_link)

  val openSupportLink = {
    val opened = runCatching { uriHandler.openUri(supportUrl) }.isSuccess
    if (!opened) {
      coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(unableToOpenLinkMessage)
      }
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is AccessKeyEvent.Verified -> {
          val message = event.message ?: context.getString(R.string.auth_success_message)
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarHostState.showSnackbar(message)
        }
      }
    }
  }

  val isLoading = uiState.status is AuthCheckStatus.Loading
  val handleAuthenticate = {
    focusManager.clearFocus()
    viewModel.authenticate()
  }

  val errorStatus = uiState.status as? AuthCheckStatus.Error
  if (errorStatus != null) {
    val reasonText =
      when {
        !errorStatus.message.isNullOrBlank() -> errorStatus.message
        errorStatus.isNetworkError -> stringResource(R.string.auth_network_error)
        errorStatus.httpCode != null ->
          stringResource(R.string.auth_invalid_message, errorStatus.httpCode)
        else -> stringResource(R.string.auth_network_error)
      }

    AlertDialog(
      onDismissRequest = viewModel::dismissError,
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      title = {
        Text(
          text = stringResource(R.string.auth_error_title),
          style = MaterialTheme.typography.titleLarge,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(text = reasonText, style = MaterialTheme.typography.bodyLarge)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.auth_error_help_prompt),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
              onClick = {
                openSupportLink()
                viewModel.dismissError()
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("support_telegram_link"),
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_telegram),
                contentDescription = stringResource(R.string.cd_telegram_icon),
                modifier = Modifier.size(32.dp),
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            openSupportLink()
            viewModel.dismissError()
          },
          shape = RoundedCornerShape(8.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("get_help_button"),
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_telegram_plane),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.action_get_help))
        }
      },
      dismissButton = {
        TextButton(
          onClick = viewModel::dismissError,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("dismiss_error_button"),
        ) {
          Text(
            text = stringResource(R.string.action_dismiss),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      // Top-right node online indicator
      Row(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(top = 12.dp, end = 24.dp)
            .testTag("node_status_indicator"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(6.dp)
              .background(color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
          text = stringResource(R.string.node_status_online),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Centered authentication stack
      Column(
        modifier =
          Modifier.align(Alignment.Center)
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        UsdtBadge(modifier = Modifier.size(96.dp))

        Spacer(modifier = Modifier.height(30.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = stringResource(R.string.prompt_access_key),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          IconButton(
            onClick = {
              coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(helpMessage)
              }
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("help_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.HelpOutline,
              contentDescription = stringResource(R.string.cd_access_key_help),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = uiState.accessKey,
          onValueChange = viewModel::onAccessKeyChange,
          enabled = !isLoading,
          placeholder = {
            Text(
              text = stringResource(R.string.hint_access_key),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          textStyle = MaterialTheme.typography.bodyLarge,
          singleLine = true,
          isError = uiState.showLengthError,
          visualTransformation = PasswordVisualTransformation(),
          shape = RoundedCornerShape(8.dp),
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Password,
              autoCorrectEnabled = false,
              imeAction = ImeAction.Done,
            ),
          keyboardActions = KeyboardActions(onDone = { handleAuthenticate() }),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              disabledContainerColor = MaterialTheme.colorScheme.surface,
              errorContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              disabledBorderColor = MaterialTheme.colorScheme.outline,
              errorBorderColor = MaterialTheme.colorScheme.error,
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              cursorColor = MaterialTheme.colorScheme.primary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("access_key_input"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.helper_min_chars),
          style = MaterialTheme.typography.bodySmall,
          color =
            if (uiState.showLengthError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth().testTag("helper_min_chars"),
        )

        Spacer(modifier = Modifier.height(22.dp))

        val accent = MaterialTheme.colorScheme.primary
        Box(
          modifier =
            Modifier.fillMaxWidth().drawBehind {
              val glowExpandX = 10.dp.toPx()
              val glowExpandY = 12.dp.toPx()
              drawRoundRect(
                brush =
                  Brush.radialGradient(
                    colors =
                      listOf(
                        accent.copy(alpha = 0.26f),
                        accent.copy(alpha = 0.08f),
                        Color.Transparent,
                      ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.65f,
                  ),
                topLeft = Offset(-glowExpandX, -glowExpandY),
                size = Size(size.width + glowExpandX * 2f, size.height + glowExpandY * 2f),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
              )
            }
        ) {
          Button(
            onClick = handleAuthenticate,
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = accent.copy(alpha = 0.7f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .height(50.dp)
                .minimumInteractiveComponentSize()
                .testTag("authenticate_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = stringResource(R.string.action_authenticate),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }

      // Bottom copyright footer
      Text(
        text = stringResource(R.string.footer_copyright),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
            .testTag("footer_copyright"),
      )
    }
  }
}

@Composable
private fun UsdtBadge(modifier: Modifier = Modifier) {
  val accent = MaterialTheme.colorScheme.primary
  val context = LocalContext.current
  val fallbackPainter = painterResource(id = R.drawable.ic_usdt_logo)
  val request =
    remember(context) {
      ImageRequest.Builder(context)
        .data(USDT_LOGO_URL)
        .setHeader("User-Agent", "FlashUSDT/1.0 (Android)")
        .crossfade(true)
        .build()
    }

  Box(
    modifier =
      modifier
        .drawBehind {
          val center = Offset(size.width / 2f, size.height / 2f)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    accent.copy(alpha = 0.30f),
                    accent.copy(alpha = 0.09f),
                    Color.Transparent,
                  ),
                center = center,
                radius = size.width * 1.15f,
              ),
            radius = size.width * 1.15f,
            center = center,
          )
        }
        .testTag("usdt_badge"),
    contentAlignment = Alignment.Center,
  ) {
    AsyncImage(
      model = request,
      contentDescription = stringResource(R.string.cd_usdt_logo),
      placeholder = fallbackPainter,
      error = fallbackPainter,
      fallback = fallbackPainter,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun AccessKeyScreenPreview() {
  FlashUsdtTheme { AccessKeyScreen() }
}
