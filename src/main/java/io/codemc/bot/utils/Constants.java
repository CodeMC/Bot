/*
 * Copyright 2021 CodeMC.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.codemc.bot.utils;

public class Constants{
    
    // Server ID
    public static final String SERVER = "405915656039694336";
    
    // Channel IDs
    public static final String REQUEST_ACCESS    = "782998340559306792";
    public static final String ACCEPTED_REQUESTS = "784119059138478080";
    public static final String REJECTED_REQUESTS = "800423355551449098";
    
    // Role IDs
    public static final String ADMINISTRATOR = "405917902865170453";
    public static final String MODERATOR     = "659568973079379971";
    public static final String AUTHOR        = "405918641859723294";
    
    // Result messages for applications
    public static final String ACCEPTED_MSG =
        "Your request has been **accepted**!\n" +
        "You will now be able to login with your GitHub Account and access the approved Repository on the CI.\n" +
        "\n" +
        "Remember to [visit our Documentation](https://docs.codemc.io) and [read our FAQ](https://docs.codemc.io/faq) " +
        "to know how to setup automatic builds!";
    public static final String REJECTED_MSG =
        "Your request has unfortunately been **rejected**.\n" +
        "Please see the below listed reason for why.\n" +
        "\n" +
        "You may re-apply for access unless mentioned so in the reason.";
}
