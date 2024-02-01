package com.binbin.androidowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.UserManagerCompat
import androidx.navigation.NavHostController


@Composable
fun UserManage(navCtrl:NavHostController){
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        val myContext = LocalContext.current
        val myDpm = myContext.getSystemService(ComponentActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val myComponent = ComponentName(myContext,MyDeviceAdminReceiver::class.java)
        val focusMgr = LocalFocusManager.current
        val userManager = myContext.getSystemService(Context.USER_SERVICE) as UserManager
        val sharedPref = LocalContext.current.getSharedPreferences("data", Context.MODE_PRIVATE)
        val isWear = sharedPref.getBoolean("isWear",false)
        Column(modifier = sections()) {
            Text(text = "用户信息", style = MaterialTheme.typography.titleLarge,color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("用户已解锁：${UserManagerCompat.isUserUnlocked(myContext)}",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
            if(VERSION.SDK_INT>=24){
                Text("支持多用户：${UserManager.supportsMultipleUsers()}",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
            }
            if(VERSION.SDK_INT>=31){
                Text("系统用户: ${UserManager.isHeadlessSystemUserMode()}",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
            }
            Spacer(Modifier.padding(vertical = if(isWear){2.dp}else{5.dp}))
            if (VERSION.SDK_INT >= 28) {
                val logoutable = myDpm.isLogoutEnabled
                Text(text = "用户可以退出 : $logoutable",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
                if(isDeviceOwner(myDpm)|| isProfileOwner(myDpm)){
                    val ephemeralUser = myDpm.isEphemeralUser(myComponent)
                    Text(text = "临时用户： $ephemeralUser",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
                }
                val affiliatedUser = myDpm.isAffiliatedUser
                Text(text = "次级用户: $affiliatedUser",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
            }
            Spacer(Modifier.padding(vertical = if(isWear){2.dp}else{5.dp}))
            Text("当前UID：${android.os.Process.myUid()}")
            Text("当前UserID：${getCurrentUserId()}")
            Text("当前用户序列号：${userManager.getSerialNumberForUser(android.os.Process.myUserHandle())}")
        }

        Column(modifier = sections()) {
            Text(text = "用户操作", style = MaterialTheme.typography.titleLarge,color = MaterialTheme.colorScheme.onPrimaryContainer)
            var idInput by remember{ mutableStateOf("") }
            var userHandleById:UserHandle by remember{ mutableStateOf(android.os.Process.myUserHandle()) }
            var useUid by remember{ mutableStateOf(false) }
            TextField(
                value = idInput,
                onValueChange = {
                    idInput=it
                    if(useUid){
                        if(idInput!=""&&VERSION.SDK_INT>=24){
                            userHandleById = UserHandle.getUserHandleForUid(idInput.toInt())
                        }
                    }else{
                        val userHandleBySerial = userManager.getUserForSerialNumber(idInput.toLong())
                        userHandleById = userHandleBySerial ?: android.os.Process.myUserHandle()
                    }
                },
                label = {Text(if(useUid){"UID"}else{"序列号"})},
                enabled = isDeviceOwner(myDpm),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {focusMgr.clearFocus()})
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .clickable(enabled = VERSION.SDK_INT>=24&&isDeviceOwner(myDpm)){useUid=!useUid}
                    .padding(end = 12.dp)
            ){
                Checkbox(
                    checked = useUid,
                    onCheckedChange = {useUid=it},
                    enabled = VERSION.SDK_INT>=24&& isDeviceOwner(myDpm)
                )
                Text(text = "使用UID(不靠谱)",modifier = Modifier.padding(bottom = 2.dp))
            }
            if(VERSION.SDK_INT>28){
                if(isProfileOwner(myDpm)&&myDpm.isAffiliatedUser){
                    Button(
                        onClick = {
                            val result = myDpm.logoutUser(myComponent)
                            Toast.makeText(myContext, userOperationResultCode(result), Toast.LENGTH_SHORT).show()
                        },
                        enabled = isProfileOwner(myDpm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("登出当前用户")
                    }
                }
                Button(
                    onClick = {
                        focusMgr.clearFocus()
                        val result = myDpm.startUserInBackground(myComponent,userHandleById)
                        Toast.makeText(myContext, userOperationResultCode(result), Toast.LENGTH_SHORT).show()
                    },
                    enabled = isDeviceOwner(myDpm),
                    modifier = Modifier.fillMaxWidth()
                ){
                    Text("在后台启动用户")
                }
            }
            Button(
                onClick = {
                    focusMgr.clearFocus()
                    if(myDpm.switchUser(myComponent,userHandleById)){
                        focusMgr.clearFocus()
                        Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isDeviceOwner(myDpm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("切换至用户")
            }
            Row(
                modifier = if(isWear){
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())}else{Modifier.fillMaxWidth()},
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Button(
                    onClick = {
                        focusMgr.clearFocus()
                        try{
                            if(VERSION.SDK_INT>=28){
                                val result = myDpm.stopUser(myComponent,userHandleById)
                                Toast.makeText(myContext, userOperationResultCode(result), Toast.LENGTH_SHORT).show()
                            }
                        }catch(e:IllegalArgumentException){
                            Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isDeviceOwner(myDpm)&&VERSION.SDK_INT>=28,
                    modifier = Modifier.fillMaxWidth(0.48F)
                ) {
                    Text("停止用户")
                }
                Button(
                    onClick = {
                        focusMgr.clearFocus()
                        if(myDpm.removeUser(myComponent,userHandleById)){
                            Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                            idInput=""
                        }else{
                            Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isDeviceOwner(myDpm),
                    modifier = Modifier.fillMaxWidth(0.92F)
                ) {
                    Text("移除用户")
                }
            }
            if(VERSION.SDK_INT<28){
                Text("停止用户需API28")
            }
        }

        Column(modifier = sections()) {
            Text(text = "工作资料", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = if(isWear){
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())}else{Modifier.fillMaxWidth()},
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Button(
                    onClick = { createWorkProfile(myContext)},
                    modifier = Modifier.fillMaxWidth(0.48F)
                ) {
                    Text("创建")
                }
                Button(
                    onClick = {
                        try{
                            myDpm.setProfileEnabled(myComponent)
                        }catch(e:SecurityException){
                            Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isProfileOwner(myDpm)||isDeviceOwner(myDpm),
                    modifier = Modifier.fillMaxWidth(0.95F)
                ) {
                    Text(text = "启用")
                }
            }
            Text("可能无法创建工作资料",style = if(isWear){MaterialTheme.typography.bodyMedium}else{MaterialTheme.typography.bodyLarge})
        }

        if(VERSION.SDK_INT>=24){
            Column(modifier = sections()) {
                var userName by remember{ mutableStateOf("") }
                Text(text = "创建用户", style = MaterialTheme.typography.titleLarge,color = MaterialTheme.colorScheme.onPrimaryContainer)
                TextField(
                    value = userName,
                    onValueChange = {userName=it},
                    label = {Text("用户名")},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = isDeviceOwner(myDpm)
                )
                var selectedFlag by remember{ mutableIntStateOf(0) }
                RadioButtonItem("无",{selectedFlag==0},{selectedFlag=0})
                RadioButtonItem("跳过创建用户向导",{selectedFlag==DevicePolicyManager.SKIP_SETUP_WIZARD},{selectedFlag=DevicePolicyManager.SKIP_SETUP_WIZARD})
                if(VERSION.SDK_INT>=28){
                    RadioButtonItem("临时用户",{selectedFlag==DevicePolicyManager.MAKE_USER_EPHEMERAL},{selectedFlag=DevicePolicyManager.MAKE_USER_EPHEMERAL})
                    RadioButtonItem("启用所有系统应用",{selectedFlag==DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED},{selectedFlag=DevicePolicyManager.LEAVE_ALL_SYSTEM_APPS_ENABLED})
                }
                var newUserHandle: UserHandle? by remember{ mutableStateOf(null) }
                Row(
                    modifier = if(isWear){Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())}else{Modifier.fillMaxWidth()},
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            newUserHandle=myDpm.createAndManageUser(myComponent,userName,myComponent,null,selectedFlag)
                            focusMgr.clearFocus()
                            if(newUserHandle!=null){
                                Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                            }else{
                                Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = isDeviceOwner(myDpm),
                        modifier = if(!isWear){
                            if(newUserHandle==null){Modifier.fillMaxWidth(1F)}else{Modifier.fillMaxWidth(0.4F)}
                        }else{Modifier}
                    ) {
                        Text("创建")
                    }
                    if(newUserHandle!=null){
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = {
                                if(myDpm.switchUser(myComponent,newUserHandle)){
                                    Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                                    navCtrl.navigate("HomePage")
                                } else{
                                    Toast.makeText(myContext, "失败", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = if(isWear){Modifier}else{Modifier.fillMaxWidth(0.97F)}
                        ) {
                            Text("切换至新用户")
                        }
                    }
                }
                if(newUserHandle!=null){
                    Text("新用户的序列号：${userManager.getSerialNumberForUser(newUserHandle)}")
                }
            }
        }else{
            Text("创建用户需安卓7")
        }
        UserSessionMessage("用户名","用户名",true,myDpm,myContext,{null},{msg ->  myDpm.setProfileName(myComponent, msg.toString())})
        if(VERSION.SDK_INT>=28){
            UserSessionMessage("用户会话开始消息","消息",false,myDpm,myContext,{myDpm.getStartUserSessionMessage(myComponent)},{msg ->  myDpm.setStartUserSessionMessage(myComponent,msg)})
            UserSessionMessage("用户会话结束消息","消息",false,myDpm,myContext,{myDpm.getEndUserSessionMessage(myComponent)},{msg ->  myDpm.setEndUserSessionMessage(myComponent,msg)})
        }
        Spacer(Modifier.padding(vertical = 30.dp))
    }
}

@Composable
fun UserSessionMessage(
    text:String,
    textField:String,
    profileOwner:Boolean,
    myDpm:DevicePolicyManager,
    myContext: Context,
    get:()->CharSequence?,
    setMsg:(msg:CharSequence?)->Unit
){
    Column(
        modifier = sections()
    ) {
        val focusMgr = LocalFocusManager.current
        var msg by remember{ mutableStateOf(if(isDeviceOwner(myDpm)||(isProfileOwner(myDpm)&&profileOwner)){ if(get()==null){""}else{get().toString()} }else{""}) }
        val sharedPref = LocalContext.current.getSharedPreferences("data", Context.MODE_PRIVATE)
        val isWear = sharedPref.getBoolean("isWear",false)
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        TextField(
            value = msg,
            onValueChange = {msg=it},
            label = {Text(textField)},
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {focusMgr.clearFocus()}),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            enabled = isDeviceOwner(myDpm)||(isProfileOwner(myDpm)&&profileOwner)
        )
        Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    focusMgr.clearFocus()
                    setMsg(msg)
                    msg = if(get()==null){""}else{get().toString()}
                    Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                },
                enabled = isDeviceOwner(myDpm)||(isProfileOwner(myDpm)&&profileOwner),
                modifier = if(isWear){Modifier}else{Modifier.fillMaxWidth(0.65F)}
            ) {
                Text("应用")
            }
            Button(
                onClick = {
                    focusMgr.clearFocus()
                    setMsg(null)
                    msg = if(get()==null){""}else{get().toString()}
                    Toast.makeText(myContext, "成功", Toast.LENGTH_SHORT).show()
                },
                enabled = isDeviceOwner(myDpm)||(isProfileOwner(myDpm)&&profileOwner),
                modifier = if(isWear){Modifier}else{Modifier.fillMaxWidth(0.95F)}
            ) {
                Text("默认")
            }
        }
    }
}

fun userOperationResultCode(result:Int): String {
    return when(result){
        UserManager.USER_OPERATION_SUCCESS->"成功"
        UserManager.USER_OPERATION_ERROR_UNKNOWN->"未知结果（失败）"
        UserManager.USER_OPERATION_ERROR_MANAGED_PROFILE->"失败：受管理的资料"
        UserManager.USER_OPERATION_ERROR_CURRENT_USER->"失败：当前用户"
        else->"未知"
    }
}

private fun createWorkProfile(myContext: Context) {
    val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
    if(VERSION.SDK_INT>=23){
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, ComponentName(myContext,MyDeviceAdminReceiver::class.java))
    }
    intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME, myContext.packageName)
    if (VERSION.SDK_INT >= 33) { intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_ALLOW_OFFLINE,true) }
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"hello")
    myContext.startActivity(intent)
}

fun getCurrentUserId(): Int {
    try {
        val uh = UserHandle::class.java.getDeclaredMethod("myUserId")
        uh.isAccessible = true
        val userId = uh.invoke(null)
        if (userId is Int) { return userId }
    } catch (ignored: Exception) { }
    return -1
}
