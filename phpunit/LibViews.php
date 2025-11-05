<?php

class LibViews
{
    public function loadLib(string $filePath, ?string $version = null): string
    {
        $extension = pathinfo($filePath, PATHINFO_EXTENSION);
        
        if ($extension === 'css') {
            $tag = '<LINK rel="stylesheet" type="text/css" href="' . $filePath;
        } else {
            $tag = '<SCRIPT src="' . $filePath;
        }
        
        if ($version !== null && $version !== '') {
            $tag .= '?v=' . $version;
        }
        
        if ($extension === 'css') {
            $tag .= '">';
        } else {
            $tag .= '">';
        }
        
        return $tag;
    }
}