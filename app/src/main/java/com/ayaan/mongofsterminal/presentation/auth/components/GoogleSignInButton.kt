package com.ayaan.mongofsterminal.presentation.auth.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ayaan.mongofsterminal.R
import com.ayaan.mongofsterminal.navigation.Route
import com.ayaan.mongofsterminal.presentation.auth.signinscreen.SignInViewModel

@Composable
fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    context: android.content.Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    navController: NavController,
    viewModel: SignInViewModel= hiltViewModel()
) {
    val icon = painterResource(id = R.drawable.google_logo)
    OutlinedButton(
        onClick = {
            viewModel.handleGoogleLogin(
                context = context,
                launcher = launcher,
                login = {
                    navController.navigate(Route.TerminalScreen.route) {
                        popUpTo(Route.SignInScreen.route) {
                            inclusive = true
                        }
                    }
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                }
            )
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        border = BorderStroke(2.dp, Color(0xFF00FF00)), // neon green border
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black,
            contentColor = Color(0xFF00FF00)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier= Modifier.width((-2).dp))
            Image(
                painter = icon,
                contentDescription = "Google logo",
                modifier = Modifier.size(24.dp) // Adjust size as needed
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CONTINUE WITH GOOGLE",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }

}