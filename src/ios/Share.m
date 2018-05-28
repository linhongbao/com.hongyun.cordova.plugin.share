#import "Share.h"

@implementation Share

-(void)pluginInitialize{
   
}


-(void)show:(CDVInvokedUrlCommand *)command{
    NSDictionary* params    = [command.arguments objectAtIndex:0];
    NSDictionary* fileArray = [params objectForKey:@"file"];
    
    UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
    pasteboard.string = [params objectForKey:@"title"];
    
    NSURL   * tempUrl;
    UIImage * tempImage;
    NSMutableArray * activityItems =[NSMutableArray array];
    for(NSString * key  in  fileArray){
        tempUrl   = [NSURL URLWithString:key];
        tempImage = [[UIImage alloc]initWithData:[NSData dataWithContentsOfURL:tempUrl]];
        [activityItems addObject:tempImage];
    }

    UIActivityViewController *activityVC = [[UIActivityViewController alloc]initWithActivityItems:activityItems applicationActivities:nil];

    

    [self.viewController presentViewController:activityVC animated:YES completion:nil];    
    CDVPluginResult* result  = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];

}
@end