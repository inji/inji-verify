import { getKeyedItems } from "../../utils/listUtils";
// PreloadImages.js
import React from 'react';

//Preload the images required for offline use case
const PreloadImages = ({ imageUrls }: {imageUrls: string[]}) => {
    return (
        <>
            {getKeyedItems(imageUrls, (url) => url).map(({ item: url, key }) => (<img key={key} src={url} className="hidden w-0" alt=""/>))}
        </>
    );
};

export default PreloadImages;
